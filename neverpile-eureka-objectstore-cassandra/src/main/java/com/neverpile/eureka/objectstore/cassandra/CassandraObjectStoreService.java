package com.neverpile.eureka.objectstore.cassandra;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.exception.VersionMismatchException;
import com.neverpile.eureka.api.exception.VersionNotFoundException;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;

import io.micrometer.core.annotation.Timed;

@Service
public class CassandraObjectStoreService implements ObjectStoreService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CassandraObjectStoreService.class);

  private static final class PurgeBackup extends CassandraTransactionalAction {
    private static final long serialVersionUID = 1L;

    private final ObjectName objectName;
    private final int version;

    private PurgeBackup(final ObjectName objectName, final int version) {
      this.objectName = objectName;
      this.version = version;
    }

    @Override
    public void run() {
      getObjectRepository().deleteByObjectNameAndByVersion(objectNameToString(objectName), version);
      getObjectDataRepository().deleteByObjectNameAndByVersion(objectNameToString(objectName), version);
    }

    @Override
    public String toString() {
      return "PurgeBackup [objectName=" + objectName + "]";
    }
  }

  private static final class RevertToBackup extends CassandraTransactionalAction {
    private static final long serialVersionUID = 1L;

    private final ObjectName objectName;
    private final int version;

    private RevertToBackup(final ObjectName objectName, final int version) {
      this.objectName = objectName;
      this.version = version;
    }

    @Override
    public void run() {
      getObjectRepository().deleteByObjectNameAndVersionGreaterThan(objectNameToString(objectName), version);
      getObjectDataRepository().deleteByObjectNameAndVersionGreaterThan(objectNameToString(objectName), version);
    }

    @Override
    public String toString() {
      return "RevertToBackup [objectName=" + objectName + "]";
    }
  }

  private static final class UndoWriteObject extends CassandraTransactionalAction {
    private static final long serialVersionUID = 1L;

    private final ObjectName objectName;

    private UndoWriteObject(final ObjectName objectName) {
      this.objectName = objectName;
    }

    @Override
    public void run() {
      getObjectRepository().deleteByObjectName(objectNameToString(objectName));
      getObjectDataRepository().deleteByObjectName(objectNameToString(objectName));
      if (objectName.to().length > 1) {
        getPrefixRepository().delete(CassandraObjectPrefix.from(objectName));
      }
    }


    @Override
    public String toString() {
      return "UndoWriteObject [objectName=" + objectName + "]";
    }
  }

  private static final class DeleteObject extends CassandraTransactionalAction {
    private static final long serialVersionUID = 1L;
    
    private final ObjectName objectName;

    private DeleteObject(final ObjectName objectName) {
      this.objectName = objectName;
    }

    @Override
    public void run() {
      delete(objectName);
    }

    private void delete(final ObjectName toDelete) {
      // delete object
      getObjectRepository().deleteByObjectName(objectNameToString(toDelete));
      getObjectDataRepository().deleteByObjectName(objectNameToString(toDelete));
      if (toDelete.to().length > 1) {
        getPrefixRepository().delete(CassandraObjectPrefix.from(toDelete));
      }
      // find recursively deeper elements and delete recursively
      getPrefixRepository() //
          .findPrefixes(toDelete) //
          .forEach(recursivePrefix -> delete(recursivePrefix.toObjectName()));
    }

    @Override
    public String toString() {
      return "DeleteObject [objectName=" + objectName + "]";
    }
  }

  private abstract static class CassandraTransactionalAction implements TransactionalAction {
    private static final long serialVersionUID = 1L;

    CassandraObjectDataRepository getObjectDataRepository() {
      return CassandraTransactionConfiguration.applicationContext.getBean(CassandraObjectDataRepository.class);
    }

    CassandraObjectRepository getObjectRepository() {
      return CassandraTransactionConfiguration.applicationContext.getBean(CassandraObjectRepository.class);
    }

    CassandraPrefixRepository getPrefixRepository() {
      return CassandraTransactionConfiguration.applicationContext.getBean(CassandraPrefixRepository.class);
    }
  }

  class CassandraStoreObject implements StoreObject {
    private final ObjectName objectName;
    private final int chunkCount;
    private final String version;

    private CassandraStoreObject(final ObjectName objectName, final String version, final int chunkCount) {
      this.objectName = objectName;
      this.chunkCount = chunkCount;
      this.version = version;
    }

    @Override
    public ObjectName getObjectName() {
      return objectName;
    }

    @Override
    public InputStream getInputStream() {
      return new ChunkedCassandraInputStream(objectNameToString(objectName), Integer.parseInt(version), chunkCount);
    }

    @Override
    public String getVersion() {
      return version;
    }
  }

  private class ChunkedCassandraInputStream extends InputStream {
    private final String objectName;
    private final int totalChunkCount;
    private int currentChunkCount = 0;
    private Iterator<CassandraObjectData> chunkIterator;
    private ByteBuffer currentChunk = null;
    private final int version;

    ChunkedCassandraInputStream(final String objectName, final int version, final int totalChunkCount) {
      this.objectName = objectName;
      this.version = version;
      this.totalChunkCount = totalChunkCount;
      this.chunkIterator = objectDataRepository.findByObjectNameAndVersion(objectName, version,
          maxResponseQueryBatchSize).iterator();
    }

    @Override
    public int read() {
      if (null == currentChunk || !currentChunk.hasRemaining())
        if (!nextChunk() || !currentChunk.hasRemaining())
          return -1;

      return currentChunk.get() & 0xff;
    }

    private boolean nextChunk() {
      if (chunkIterator.hasNext()) {
        currentChunk = chunkIterator.next().getData();
        currentChunkCount++;
        return true;
      } else if (totalChunkCount > currentChunkCount) {
        chunkIterator = objectDataRepository.findByObjectNameAndChunkNoGreaterThanEqual(objectName, version,
            currentChunkCount, maxResponseQueryBatchSize).iterator();
        if(!chunkIterator.hasNext())
          return false;
        
        currentChunk = chunkIterator.next().getData();
        currentChunkCount++;
        return true;
      } else
        return false;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      if (null == currentChunk || !currentChunk.hasRemaining())
        if (!nextChunk())
          return -1;

      int toGet = Math.min(currentChunk.remaining(), len);
      currentChunk.get(b, off, toGet);
      return toGet;
    }

  }

  @Autowired
  private TransactionWAL wal;

  @Autowired
  private CassandraObjectRepository objectRepository;

  @Autowired
  private CassandraObjectDataRepository objectDataRepository;

  @Autowired
  private CassandraPrefixRepository prefixRepository;

  /**
   * 1 MB in Bytes.
   */
  private final int MB = 1024 * 1024;

  // Configuration variables:

  /**
   * Maximum size of a single chunk of stored in Cassandra. <br>
   * maxBufferSize * maxResponseQueryBatchSize < 256 MB. <br>
   * Default: 1 MB.
   */
  private int maxBufferSize = MB;

  /**
   * Maximum batch size for a a single request. <br>
   * maxBufferSize * maxResponseQueryBatchSize < 256 MB. <br>
   * Default: 100.
   */
  private int maxResponseQueryBatchSize = 100;

  /**
   * Maximum batch size for grouping Cassandra request queries. <br>
   * maxBufferSize * maxRequestQueryBatchSize < 15 MB. <br>
   * Default: 10.
   */
  private int maxRequestQueryBatchSize = 10;

  int getMaxResponseQueryBatchSize() {
    return maxResponseQueryBatchSize;
  }

  void setMaxResponseQueryBatchSize(final int maxResponseQueryBatchSize) {
    if (maxBufferSize * maxResponseQueryBatchSize < 256 * MB) {
      this.maxResponseQueryBatchSize = maxResponseQueryBatchSize;
    } else {
      this.maxResponseQueryBatchSize = ((256 * MB) / maxBufferSize) - 1;
    }
  }

  int getMaxBufferSize() {
    return maxBufferSize;
  }

  void setMaxBufferSize(final int maxBufferSize) {
    if (maxBufferSize * maxResponseQueryBatchSize < 256 * MB && maxBufferSize * maxRequestQueryBatchSize < 15 * MB) {
      this.maxBufferSize = maxBufferSize;
    } else {
      this.maxBufferSize = Math.min(((256 * MB) / maxResponseQueryBatchSize) - 1, (15 * MB) / maxRequestQueryBatchSize);
    }
  }

  int getMaxRequestQueryBatchSize() {
    return maxRequestQueryBatchSize;
  }

  void setMaxRequestQueryBatchSize(final int maxRequestQueryBatchSize) {
    if (maxBufferSize * maxRequestQueryBatchSize < 15 * MB) {
      this.maxRequestQueryBatchSize = maxRequestQueryBatchSize;
    } else {
      this.maxRequestQueryBatchSize = (15 * MB) / maxBufferSize;
    }
  }

  void truncateObjectTable() {
    objectRepository.deleteAll();
  }

  void truncateDataTable() {
    objectDataRepository.deleteAll();
  }

  void truncatePrefixTable() {
    prefixRepository.deleteAll();
  }

  @Override
  @Timed(description = "put object store element", extraTags = {
      "subsystem", "cassandra.object-store"
  }, value = "eureka.cassandra.object-store.put")
  public void put(final ObjectName objectName, final String versionString, final InputStream content,
      final long length) {
    int version;
    if (versionString.equals(NEW_VERSION)) {
      version = 0;
    } else {
      version = Integer.parseInt(versionString);
    }
    Optional<CassandraObject> object = findObject(objectName);
    int currentVersion = 1;
    if (object.isPresent()) {
      int oldVersion = object.get().getVersion();
      if (oldVersion != version) {
        throw new VersionMismatchException("Can't Put", String.valueOf(version), String.valueOf(oldVersion));
      }
      wal.appendCommitAction(new PurgeBackup(objectName, oldVersion));
      wal.appendUndoAction(new RevertToBackup(objectName, oldVersion));
      currentVersion = oldVersion + 1;
    } else {
      if (0 != version) {
        throw new VersionNotFoundException("Can't Put", String.valueOf(version));
      }
      wal.appendUndoAction(new UndoWriteObject(objectName));
      if (objectName.to().length > 1) {
        savePrefixesToRepo(objectName);
      }
    }
    try {
      saveObjectToRepo(objectName, content, currentVersion, length);
    } catch (IOException e) {
      LOGGER.warn("Can't put: {}", objectName, e);
      throw new ObjectStoreException(objectName, "Can't put", e);
    }
  }

  private void savePrefixesToRepo(final ObjectName objectName) {
    ArrayList<CassandraObjectPrefix> prefixes = new ArrayList<>();
    prefixes.add(CassandraObjectPrefix.from(objectName));
    for (int i = objectName.to().length - 1; i > 1; i--) {
      prefixes.add(CassandraObjectPrefix.from(ObjectName.of(Arrays.copyOf(objectName.to(), i))));
    }

    prefixRepository.saveAll(prefixes);
  }

  private void saveObjectToRepo(final ObjectName objectName, final InputStream content, final int version,
      final long length) throws IOException {
    byte[] bytes = new byte[getBufferSize(length)];
    int chunkNo = 0;

    Stack<CassandraObjectData> query = new Stack<>();
    int read;
    while ((read = readAll(bytes, content)) > 0) {
      query.push(
          new CassandraObjectData(objectNameToString(objectName), version, chunkNo, ByteBuffer.wrap(bytes, 0, read)));
      if (query.size() >= maxRequestQueryBatchSize) {
        objectDataRepository.saveAll(query);
        query = new Stack<>();
      }
      bytes = new byte[getBufferSize(length)];
      chunkNo++;
    }

    if (!query.isEmpty()) {
      objectDataRepository.saveAll(query);
    }

    objectRepository.save(new CassandraObject(objectNameToString(objectName), version, chunkNo));
  }

  private int readAll(final byte[] buffer, final InputStream is) throws IOException {
    int read = 0;

    while (read < buffer.length) {
      int r = is.read(buffer, read, buffer.length - read);
      if (r <= 0)
        break;
      read += r;
    }

    return read;
  }

  private int getBufferSize(final long length) {
    if (length <= 0)
      return maxBufferSize;
    return (int) Math.min(length, maxBufferSize);
  }

  @Override
  @Timed(description = "list object store elements", extraTags = {
      "subsystem", "cassandra.object-store"
  }, value = "eureka.cassandra.object-store.list")
  public Stream<StoreObject> list(final ObjectName prefix) {
    if (prefix.to().length == 0) {
      return listAll();
    }

    return prefixRepository.findPrefixes(prefix) //
        .flatMap(p -> {
          ObjectName objectName = p.toObjectName();

          return findObject(objectName) //
              .map(o -> {
                CassandraStoreObject storeObject = createStoreObject(o);

                // is this just an object or also another prefix?
                int count = prefixRepository.countSuffixes(
                    String.join(CassandraObjectPrefix.PREFIX_DELIMITER, objectName.to()));

                if (count > 0)
                  return Stream.of(storeObject, createPrefixObject(p));
                else
                  return Stream.of(storeObject);
              }) //
              .orElse(Stream.of(createPrefixObject(p)));
        });
  }

  private CassandraStoreObject createStoreObject(final CassandraObject o) {
    return new CassandraStoreObject(objectNameFromString(o.getObjectName()), String.valueOf(o.getVersion()),
        o.getDataChunkCount());
  }

  private Optional<CassandraObject> findObject(final ObjectName objectName) {
    return objectRepository.findByObjectName(objectNameToString(objectName));
  }

  private StoreObject createPrefixObject(final CassandraObjectPrefix prefix) {
    return new StoreObject() {
      @Override
      public String getVersion() {
        return null;
      }

      @Override
      public ObjectName getObjectName() {
        return prefix.toObjectName();
      }

      @Override
      public InputStream getInputStream() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public Stream<StoreObject> listAll() {
    Stream<CassandraObject> stream = objectRepository.findAllCassandraObjects();
    return stream.map((object) -> createStoreObject(object));
  }

  @Override
  @Timed(description = "retrieve object store element", extraTags = {
      "subsystem", "cassandra.object-store"
  }, value = "eureka.cassandra.object-store.get")
  public StoreObject get(final ObjectName objectName) {
    Optional<CassandraObject> response = findObject(objectName);
    if (response.isPresent()) {
      CassandraObject object = response.get();
      return new CassandraStoreObject(objectName, String.valueOf(object.getVersion()), object.getDataChunkCount());
    }
    return null;
  }

  @Override
  @Timed(description = "delete object store element", extraTags = {
      "subsystem", "cassandra.object-store"
  }, value = "eureka.cassandra.object-store.delete")
  public void delete(final ObjectName objectName) {
    wal.appendCommitAction(new DeleteObject(objectName));
  }

  @Override
  @Timed(description = "check object store element exists", extraTags = {
      "subsystem", "cassandra.object-store"
  }, value = "eureka.cassandra.object-store.check-exists")
  public boolean checkObjectExists(final ObjectName objectName) {
    Optional<CassandraObject> objects = findObject(objectName);
    return objects.isPresent();
  }

  private final static String OBJECT_NAME_DELIMITER = "\t";

  private static String objectNameToString(final ObjectName objectName) {
    return String.join(OBJECT_NAME_DELIMITER, objectName.to());
  }

  private static ObjectName objectNameFromString(final String objectNameString) {
    return ObjectName.of(objectNameString.split(OBJECT_NAME_DELIMITER));
  }
}