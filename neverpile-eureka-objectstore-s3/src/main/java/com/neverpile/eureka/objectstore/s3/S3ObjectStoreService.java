package com.neverpile.eureka.objectstore.s3;

import static com.neverpile.eureka.util.ObjectNames.escape;
import static com.neverpile.eureka.util.ObjectNames.unescape;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.neverpile.common.opentracing.Tag;
import com.neverpile.common.opentracing.TraceInvocation;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.exception.VersionMismatchException;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;
import com.neverpile.eureka.util.ObjectNames;

import io.micrometer.core.annotation.Timed;
import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3ObjectStoreService implements ObjectStoreService {
  public static class ObjectNameMapper implements Function<ObjectName, String> {
    @Override
    public String apply(final ObjectName n) {
      return n.stream().map(ObjectNames::escape).collect(joining(NAME_DELIMITER));
    }
  }

  private static final String BACKUP_SUFFIX = ".%BACKUP%";

  private static final String NAME_DELIMITER = "/";

  private static final Pattern DELIMITER_SPLIT_PATTERN = Pattern.compile(Pattern.quote(NAME_DELIMITER));

  private abstract static class S3TXAction implements TransactionalAction {
    private static final long serialVersionUID = 1L;

    /**
     * We use this static in order to propagate the connection configuration to the (static)
     * actions.
     * <p>
     * FIXME: this is rather ugly. The alternative would be to make the whole S3 configuration
     * serializable and create connections upon action execution. However, the S3 configuration
     * isn't serializable per se making this endeavor rather tedious.
     */
    private static transient S3ConnectionConfiguration connectionConfiguration;

    public S3ConnectionConfiguration getConnectionConfiguration() {
      if (null == connectionConfiguration)
        throw new IllegalStateException("Connection configuration not yet set");

      return connectionConfiguration;
    }

    public static void setConnectionConfiguration(final S3ConnectionConfiguration connectionConfiguration) {
      S3TXAction.connectionConfiguration = connectionConfiguration;
    }
  }

  private static final class PurgeObjectAction extends S3TXAction {
    private static final long serialVersionUID = 1L;

    private final String bucket;
    private final String key;

    public PurgeObjectAction(final String bucket, final String key) {
      this.bucket = bucket;
      this.key = key;
    }

    @Override
    public void run() {
      getConnectionConfiguration().createClient().deleteObject(builder -> builder.bucket(bucket).key(key));
    }
  }

  private static final class MoveObjectAction extends S3TXAction {
    private static final long serialVersionUID = 1L;

    private final String bucket;

    private final String fromKey;

    private final String toKey;

    public MoveObjectAction(final String bucketName, final String fromKey, final String toKey) {
      this.bucket = bucketName;
      this.fromKey = fromKey;
      this.toKey = toKey;
    }

    @Override
    public void run() {
      S3Client c = getConnectionConfiguration().createClient();
      c.copyObject(
          builder -> builder.sourceBucket(bucket).sourceKey(fromKey).destinationKey(toKey).destinationBucket(bucket));
      c.deleteObject(builder -> builder.bucket(bucket).key(fromKey));
    }
  }

  @Autowired
  private S3ConnectionConfiguration connectionConfiguration;

  @Autowired
  private TransactionWAL writeAheadLog;

  private S3Client s3client;

  @PostConstruct
  private void init() {
    s3client = connectionConfiguration.createClient();
    S3TXAction.setConnectionConfiguration(connectionConfiguration);
  }

  @Override
  @Timed(description = "put object store element", extraTags = {"subsystem", "s3.object-store"
  }, value = "eureka.s3.object-store.put")
  @TraceInvocation
  public void put(@Tag(name = "key", valueAdapter = ObjectNameMapper.class) final ObjectName objectName,
      final String version, final InputStream content, @Tag(name = "length") final long length) {
    String bucket = connectionConfiguration.getDefaultBucketName();
    String key = toKey(objectName);

    // fetch existing object metadata
    String currentVersion = getCurrentVersion(bucket, key);
    boolean alreadyExists = !currentVersion.equals(NEW_VERSION);

    // check for version mismatch
    if (!currentVersion.equals(version)) {
      throw new VersionMismatchException("Can't Put", version, currentVersion);
    }

    if (alreadyExists) {
      createBackup(bucket, key);
    } else {
      writeAheadLog.appendUndoAction(new PurgeObjectAction(bucket, key));
    }

    PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder().bucket(bucket).key(key);

    //    Request Body
    RequestBody requestBody;
    if (length > 0) {
      putObjectRequestBuilder.contentLength(length);
      requestBody = RequestBody.fromInputStream(content, length);
    } else {
      // Read stream into memory
      byte[] bytes;
      try {
        bytes = content.readAllBytes();
        requestBody = RequestBody.fromBytes(bytes);
      } catch (IOException e) {
        throw new RuntimeException("Failed to read input stream", e);
      }
    }
    PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();
    s3client.putObject(putObjectRequest, requestBody);
  }

  private String getCurrentVersion(final String bucket, final String key) {
    String currentVersion = NEW_VERSION;
    try {
      HeadObjectRequest headRequest = HeadObjectRequest.builder().bucket(bucket).key(key).build();

      HeadObjectResponse headResponse = s3client.headObject(headRequest);
      currentVersion = headResponse.eTag();
    } catch (NoSuchKeyException e) {
      if (e.statusCode() != HttpStatus.NOT_FOUND.value())
        throw e;

      // else fall out
    }
    return currentVersion;
  }

  private String toKey(final ObjectName objectName) {
    return objectName.stream().map(s -> escape(s)).collect(joining(NAME_DELIMITER));
  }

  private final class ObjectListingSpliterator extends Spliterators.AbstractSpliterator<StoreObject> {
    private ListObjectsV2Response listing;
    private Iterator<S3Object> summaries;
    private Iterator<String> prefixes;
    private String continuationToken;

    public ObjectListingSpliterator(final ListObjectsV2Response listing) {
      super(listing.isTruncated() ? Long.MAX_VALUE : listing.contents().size(), Spliterator.ORDERED);
      this.listing = listing;
      this.continuationToken = listing.nextContinuationToken();
      summaries = listing.contents().iterator();
      prefixes = listing.commonPrefixes().stream().map(CommonPrefix::prefix).iterator();
    }

    @Override
    public boolean tryAdvance(final Consumer<? super StoreObject> action) {
      if (summaries.hasNext()) {
        S3Object s;
        do {
          s = summaries.next();
        } while (s.key().endsWith(BACKUP_SUFFIX) && summaries.hasNext()); // hide backups

        if (!s.key().endsWith(BACKUP_SUFFIX)) {
          action.accept(toStoreObject(s));
          return true;
        }
      }

      if (prefixes.hasNext()) {
        String p = prefixes.next();
        action.accept(toStoreObject(p));
        return true;
      }

      if (listing.isTruncated() && continuationToken != null) {
        // fetch next chunk
        ListObjectsV2Request nextRequest = ListObjectsV2Request.builder().bucket(
                listing.name()) // Note: you may need to store bucket name separately
            .continuationToken(continuationToken).build();

        listing = s3client.listObjectsV2(nextRequest);
        continuationToken = listing.nextContinuationToken();
        summaries = listing.contents().iterator();
        prefixes = listing.commonPrefixes().stream().map(CommonPrefix::prefix).iterator();

        return tryAdvance(action);
      }

      return false;
    }

    private StoreObject toStoreObject(final S3Object s) {
      return new StoreObject() {
        @Override
        public String getVersion() {
          return s.eTag();
        }

        @Override
        public ObjectName getObjectName() {
          return toObjectName(s.key());
        }

        @Override
        public InputStream getInputStream() {
          return S3ObjectStoreService.this.get(getObjectName()).getInputStream();
        }
      };
    }

    private StoreObject toStoreObject(final String prefix) {
      return new StoreObject() {
        @Override
        public String getVersion() {
          return null;
        }

        @Override
        public ObjectName getObjectName() {
          return toObjectName(prefix);
        }

        @Override
        public InputStream getInputStream() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }

  @Override
  @TraceInvocation
  public Stream<StoreObject> list(
      @Tag(name = "prefix", valueAdapter = ObjectNameMapper.class) final ObjectName prefix) {
    String prefixKey = toKey(prefix);

    ListObjectsV2Request lor = ListObjectsV2Request.builder().bucket(
        connectionConfiguration.getDefaultBucketName()).prefix(
        prefixKey.isEmpty() ? prefixKey : prefixKey + NAME_DELIMITER).delimiter(NAME_DELIMITER).build();

    return StreamSupport.stream(new ObjectListingSpliterator(s3client.listObjectsV2(lor)), false);
  }

  @Override
  @Timed(description = "get object store element", extraTags = {"subsystem", "s3.object-store"
  }, value = "eureka.s3.object-store.get")
  @TraceInvocation
  public StoreObject get(@Tag(name = "key", valueAdapter = ObjectNameMapper.class) final ObjectName objectName) {
    try {
      GetObjectRequest getRequest = GetObjectRequest.builder().bucket(
          connectionConfiguration.getDefaultBucketName()).key(toKey(objectName)).build();

      final ResponseInputStream<GetObjectResponse> responseStream = s3client.getObject(getRequest);
      final GetObjectResponse response = responseStream.response();

      return new StoreObject() {
        @Override
        public String getVersion() {
          return response.eTag();
        }

        @Override
        public ObjectName getObjectName() {
          return objectName;
        }

        @Override
        public InputStream getInputStream() {
          return responseStream;
        }
      };
    } catch (NoSuchKeyException e) {
      // Object doesn't exist
      return null;
    } catch (S3Exception e) {
      if (e.statusCode() != HttpStatusCode.NOT_FOUND) {
        throw e;
      }
      return null;
    }
  }

  @Override
  @Timed(description = "delete object store element", extraTags = {"subsystem", "s3.object-store"
  }, value = "eureka.s3.object-store.delete")
  public void delete(final ObjectName prefix) {
    ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(
        connectionConfiguration.getDefaultBucketName()).prefix(toKey(prefix)).build();

    StreamSupport.stream(new ObjectListingSpliterator(s3client.listObjectsV2(listRequest)), false).forEach(
        o -> deleteSingleObject(o.getObjectName()));
  }

  private void deleteSingleObject(final ObjectName objectName) {
    String bucket = connectionConfiguration.getDefaultBucketName();
    String key = toKey(objectName);

    // fetch current version
    String currentVersion = getCurrentVersion(bucket, key);
    boolean alreadyExists = !currentVersion.equals(NEW_VERSION);

    if (alreadyExists) {
      createBackup(bucket, key);

      // delete object
      DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(bucket).key(key).build();

      s3client.deleteObject(deleteRequest);
    }
  }

  /**
   * Backup the object for the given key and register transactional actions to purge the backup upon
   * commit or restore the object upon rollback.
   *
   * @param bucket the destination bucket
   * @param key    the S3 object key
   */
  private void createBackup(final String bucket, final String key) {
    String backupKey = key + BACKUP_SUFFIX;

    writeAheadLog.appendCommitAction(new PurgeObjectAction(bucket, backupKey));
    writeAheadLog.appendUndoAction(new MoveObjectAction(bucket, key + BACKUP_SUFFIX, key));

    CopyObjectRequest copyRequest = CopyObjectRequest.builder().sourceBucket(bucket).sourceKey(key).destinationBucket(
        bucket).destinationKey(backupKey).build();

    s3client.copyObject(copyRequest);
  }

  @Override
  @Timed(description = "verify object store element exists", extraTags = {"subsystem", "s3.object-store"
  }, value = "eureka.s3.object-store.check-exists")
  @TraceInvocation
  public boolean checkObjectExists(
      @Tag(name = "key", valueAdapter = ObjectNameMapper.class) final ObjectName objectName) {
    String bucket = connectionConfiguration.getDefaultBucketName();
    String key = toKey(objectName);

    return !getCurrentVersion(bucket, key).equals(NEW_VERSION);
  }

  private ObjectName toObjectName(final String key) {
    String[] components = DELIMITER_SPLIT_PATTERN.split(key);
    for (int i = 0; i < components.length; i++) {
      components[i] = unescape(components[i]);
    }
    return ObjectName.of(components);
  }
}
