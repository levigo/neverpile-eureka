package com.neverpile.eureka.objectstore.s3;

import static com.neverpile.eureka.util.ObjectNames.escape;
import static com.neverpile.eureka.util.ObjectNames.unescape;
import static java.util.stream.Collectors.joining;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.neverpile.common.opentracing.Tag;
import com.neverpile.common.opentracing.TraceInvocation;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.exception.VersionMismatchException;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;

import io.micrometer.core.annotation.Timed;

public class S3ObjectStoreService implements ObjectStoreService {
  public static class ObjectNameMapper implements Function<ObjectName, String> {
    @Override
    public String apply(final ObjectName n) {
      return n.stream().map(s -> escape(s)).collect(joining(NAME_DELIMITER));
    }
  }

  private static final String BACKUP_SUFFIX = ".%BACKUP%";

  private static final String NAME_DELIMITER = "/";

  private static Pattern DELIMITER_SPLIT_PATTERN = Pattern.compile(Pattern.quote(NAME_DELIMITER));

  private abstract static class S3TXAction implements TransactionalAction {
    private static final long serialVersionUID = 1L;

    /**
     * We use this static in order to propagate the connection configuration to the (static)
     * actions.
     * 
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
      getConnectionConfiguration().createClient().deleteObject(bucket, key);
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
      AmazonS3 c = getConnectionConfiguration().createClient();
      c.copyObject(bucket, fromKey, bucket, toKey);
      c.deleteObject(bucket, fromKey);
    }
  }

  @Autowired
  private S3ConnectionConfiguration connectionConfiguration;

  @Autowired
  private TransactionWAL writeAheadLog;
  
  private AmazonS3 s3client;

  @PostConstruct
  private void init() {
    s3client = connectionConfiguration.createClient();
    S3TXAction.setConnectionConfiguration(connectionConfiguration);
  }

  @Override
  @Timed(description = "put object store element", extraTags = {
      "subsystem", "s3.object-store"
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

    ObjectMetadata metadata = new ObjectMetadata();
    if (length >= 0)
      metadata.setContentLength(length);

    s3client.putObject(bucket, key, content, metadata);
  }

  private String getCurrentVersion(final String bucket, final String key) {
    String currentVersion = NEW_VERSION;
    try {
      ObjectMetadata objectMetadata = s3client.getObjectMetadata(bucket, key);
      currentVersion = objectMetadata.getETag();
    } catch (AmazonS3Exception e) {
      if (e.getStatusCode() != HttpStatus.NOT_FOUND.value())
        throw e;

      // else fall out
    }
    return currentVersion;
  }

  private String toKey(final ObjectName objectName) {
    return objectName.stream().map(s -> escape(s)).collect(joining(NAME_DELIMITER));
  }

  private final class ObjectListingSpliterator extends Spliterators.AbstractSpliterator<StoreObject> {
    private ObjectListing listing;
    private Iterator<S3ObjectSummary> summaries;
    private Iterator<String> prefixes;

    public ObjectListingSpliterator(final ObjectListing listing) {
      super(listing.isTruncated() ? Long.MAX_VALUE : listing.getObjectSummaries().size(), Spliterator.ORDERED);
      this.listing = listing;
      summaries = listing.getObjectSummaries().iterator();
      prefixes = listing.getCommonPrefixes().iterator();
    }

    @Override
    public boolean tryAdvance(final Consumer<? super StoreObject> action) {
      if (summaries.hasNext()) {
        S3ObjectSummary s;
        do {
          s = summaries.next();
        } while (s.getKey().endsWith(BACKUP_SUFFIX) && summaries.hasNext()); // hide backups

        action.accept(toStoreObject(s));
        return true;
      }

      if (prefixes.hasNext()) {
        String p = prefixes.next();
        action.accept(toStoreObject(p));
        return true;
      }

      if (listing.isTruncated()) {
        // fetch next chunnk
        listing = s3client.listNextBatchOfObjects(listing);
        summaries = listing.getObjectSummaries().iterator();
        prefixes = listing.getCommonPrefixes().iterator();

        return tryAdvance(action);
      }

      return false;
    }

    private StoreObject toStoreObject(final S3ObjectSummary s) {
      return new StoreObject() {
        @Override
        public String getVersion() {
          return s.getETag();
        }

        @Override
        public ObjectName getObjectName() {
          return toObjectName(s.getKey());
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

    ListObjectsRequest lor = new ListObjectsRequest();
    lor.setBucketName(connectionConfiguration.getDefaultBucketName());

    lor.setPrefix(prefixKey.isEmpty() ? prefixKey : prefixKey + NAME_DELIMITER);
    lor.setDelimiter(NAME_DELIMITER);

    return StreamSupport.stream(new ObjectListingSpliterator(s3client.listObjects(lor)), false);
  }

  @Override
  @Timed(description = "get object store element", extraTags = {
      "subsystem", "s3.object-store"
  }, value = "eureka.s3.object-store.get")
  @TraceInvocation
  public StoreObject get(@Tag(name = "key", valueAdapter = ObjectNameMapper.class) final ObjectName objectName) {
    try {
      final S3Object object = s3client.getObject(connectionConfiguration.getDefaultBucketName(), toKey(objectName));

      return new StoreObject() {
        @Override
        public String getVersion() {
          return object.getObjectMetadata().getETag();
        }

        @Override
        public ObjectName getObjectName() {
          return objectName;
        }

        @Override
        public InputStream getInputStream() {
          return object.getObjectContent();
        }
      };
    } catch (AmazonS3Exception e) {
      if (e.getStatusCode() != HttpStatus.NOT_FOUND.value())
        throw e;

      return null;
    }
  }

  @Override
  @Timed(description = "delete object store element", extraTags = {
      "subsystem", "s3.object-store"
  }, value = "eureka.s3.object-store.delete")
  public void delete(final ObjectName prefix) {
    StreamSupport.stream(new ObjectListingSpliterator(
        s3client.listObjects(connectionConfiguration.getDefaultBucketName(), toKey(prefix))), false) //
        .forEach(o -> deleteSingleObject(o.getObjectName()));
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
      s3client.deleteObject(bucket, key);
    }
  }

  /**
   * Backup the object for the given key and register transactional actions to purge the backup upon
   * commit or restore the object upon rollback.
   * 
   * @param bucket the destination bucket
   * @param key the S3 object key
   */
  private void createBackup(final String bucket, final String key) {
    String backupKey = key + BACKUP_SUFFIX;

    writeAheadLog.appendCommitAction(new PurgeObjectAction(bucket, backupKey));
    writeAheadLog.appendUndoAction(new MoveObjectAction(bucket, key + BACKUP_SUFFIX, key));

    s3client.copyObject(bucket, key, bucket, backupKey);
  }

  @Override
  @Timed(description = "verify object store element exists", extraTags = {
      "subsystem", "s3.object-store"
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
