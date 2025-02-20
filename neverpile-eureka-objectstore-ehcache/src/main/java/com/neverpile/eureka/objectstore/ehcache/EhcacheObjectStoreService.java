package com.neverpile.eureka.objectstore.ehcache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.exception.VersionMismatchException;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.tx.wal.TransactionWAL;

public class EhcacheObjectStoreService implements ObjectStoreService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EhcacheObjectStoreService.class);
  private static EhcacheObjectStoreService instance;

  private Cache<String, byte[]> cache;
  private Cache<String, byte[]> tempCache;
  private final Map<String, List<String>> latestVersions = new ConcurrentHashMap<>();

  @Autowired
  private TransactionWAL wal;

  @Autowired
  private EhcacheConfig ehcacheConfig;

  public static EhcacheObjectStoreService getInstance() {
    return instance;
  }

  @PostConstruct
  public void init() {
    Path root = FileSystems.getDefault().getPath(ehcacheConfig.getRootPath()).normalize();
    File rootPathAsFile = root.toFile();
    LOGGER.info("-----");
    LOGGER.info("Initializing neverpile eureka - Ehcache Storage Bridge ...");
    LOGGER.info("Root directory for storing objects: '{}'", root.toAbsolutePath());
    LOGGER.info("configured disk space: {} GB", Integer.parseInt(ehcacheConfig.getDiskSize()) / 1000);
    LOGGER.info("Free space left: {} GB", rootPathAsFile.getFreeSpace() / 1000000000);
    LOGGER.info("Permissions on root directory: {}{}{}", //
        rootPathAsFile.canRead() ? "Read, " : "", //
        rootPathAsFile.canWrite() ? "Write, " : "", //
        rootPathAsFile.canExecute() ? "eXecute" : "");
    LOGGER.info("-----");
    if (ehcacheConfig.isPersistent()) {
      throw new IllegalStateException("Incompatible configuration: when multi-versioning is enabled, persistence is not possible with Ehcache");
    }

    CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .with(CacheManagerBuilder.persistence(ehcacheConfig.getRootPath()))
        .build(true);

    // Listener to remove version info if object expired
    CacheEventListenerConfigurationBuilder cacheEventListenerConfiguration = CacheEventListenerConfigurationBuilder
        .newEventListenerConfiguration((CacheEventListener<String, byte[]>) event -> {
          LOGGER.debug("Removing expired entry {}", event.getKey());
          deleteFromCache(ObjectName.of(event.getKey().substring(0, event.getKey().length() - 6).split(EhcacheHelper.SEPARATOR)));
        }, EventType.EXPIRED, EventType.EVICTED)
        .unordered().asynchronous();

    CacheConfiguration<String, byte[]> configuration = CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
            ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(Integer.parseInt(ehcacheConfig.getHeapEntries()), EntryUnit.ENTRIES)
                .disk(Integer.parseInt(ehcacheConfig.getDiskSize()), MemoryUnit.MB)
        )
        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(ehcacheConfig.getExpiryTime()))
        .withService(cacheEventListenerConfiguration)
        .build();
    cache = cacheManager.createCache("neverpile-eureka", configuration);

    // secondary cache to undo deletion
    CacheConfiguration<String, byte[]> tempConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
        ResourcePoolsBuilder.newResourcePoolsBuilder()
            .heap(Integer.parseInt(ehcacheConfig.getHeapEntries()) / 4, EntryUnit.ENTRIES)
    ).withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.of(2, ChronoUnit.SECONDS))).build();
    tempCache = cacheManager.createCache("neverpile-eureka-temp", tempConfiguration);
    instance = this;
  }

  @Override
  public void put(ObjectName objectName, String version, InputStream content) {
    addToCache(objectName, version, content);
    wal.appendUndoAction(new RevertAddAction(objectName));
  }

  @Override
  public void put(ObjectName objectName, String version, InputStream content, long length) {
    put(objectName, version, content);
  }

  /**
   * Does the actual put but without the transaction stuff
   *
   * @param objectName
   * @param version
   * @param content
   */
  public void addToCache(ObjectName objectName, String version, InputStream content) {
    String fullVersion;
    String latest = getLatestVersionForObject(objectName);
    // only accept new version or current version, can't override previous or future version
    if (ObjectStoreService.NEW_VERSION.equals(version)) {
      fullVersion = String.format("%06X", 0);
    } else {
      fullVersion = version;
      if (!fullVersion.equals(latest)) {
        throw new VersionMismatchException("mismatch", latest, fullVersion);
      }
    }
    fullVersion = String.format("%06X", Integer.parseInt(fullVersion) + 1);

    String readableObjectName = EhcacheHelper.getReadableObjectName(objectName);
    LOGGER.debug("Adding {} {}", readableObjectName, fullVersion);
    try {
      cache.put(readableObjectName + fullVersion, content.readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // keep track of versions
    List<String> versions = latestVersions.computeIfAbsent(readableObjectName, k -> new LinkedList<>());
    versions.add(fullVersion);
  }

  @Override
  public Stream<StoreObject> list(ObjectName prefix) {
    return list(prefix, false);
  }

  /**
   * Lists the Objects
   *
   * @param prefix
   * @param withObjectItself if true, the actual object is also listed if present, otherwise only siblings will be listed
   * @return
   */
  public Stream<StoreObject> list(ObjectName prefix, boolean withObjectItself) {
    String prefixReadable = EhcacheHelper.getReadableObjectName(prefix);
    List<StoreObject> result = new ArrayList<>();
    List<String> keySet = new ArrayList<>(latestVersions.keySet());
    // Collect objects
    for (String objectName : keySet) {
      if (objectName.equals(prefixReadable)) {
        if (withObjectItself) {
          result.add(new EhcacheStoreObject(ObjectName.of(objectName.split(EhcacheHelper.SEPARATOR)),
              getLatestVersionForObject(ObjectName.of(objectName.split(EhcacheHelper.SEPARATOR))), cache));
        }
      } else if (objectName.startsWith(prefixReadable)) {
        String objectNameWithoutPrefix = objectName.substring(prefixReadable.length() + 1);
        if (!objectNameWithoutPrefix.contains("#")) {
          result.add(new EhcacheStoreObject(ObjectName.of(objectName.split(EhcacheHelper.SEPARATOR)),
              getLatestVersionForObject(ObjectName.of(objectName.split(EhcacheHelper.SEPARATOR))), cache));
        }
      }
    }
    // Collect prefixes
    List<String> prefixes = new ArrayList<>();
    for (String objectName : keySet) {
      if (objectName.startsWith(prefixReadable) && !objectName.equals(prefixReadable)) {
        String objectNameWithoutPrefix = objectName.substring(prefixReadable.length() + 1);
        if (objectNameWithoutPrefix.contains("#")) {
          String prefixName = objectNameWithoutPrefix.substring(0, objectNameWithoutPrefix.indexOf("#"));
          if (!prefixes.contains(prefixName)) {
            prefixes.add(prefixName);
          }
        }
      }
    }
    // Add prefixes
    for (String prefixName : prefixes) {
      result.add(new StoreObject() {
        @Override
        public String toString() {
          return getObjectName() + " " + getVersion();
        }

        @Override
        public ObjectName getObjectName() {
          String[] elems = new String[prefix.to().length + 1];
          System.arraycopy(prefix.to(), 0, elems, 0, prefix.to().length);
          elems[elems.length - 1] = prefixName;
          return ObjectName.of(elems);
        }

        @Override
        public InputStream getInputStream() {
          throw new UnsupportedOperationException();
        }

        @Override
        public String getVersion() {
          return null;
        }

      });
    }
    return result.stream();
  }

  @Override
  public StoreObject get(ObjectName objectName) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Getting {} {}", EhcacheHelper.getReadableObjectName(objectName),
          getLatestVersionForObject(objectName));
    }
    if (checkObjectExists(objectName)) {
      return new EhcacheStoreObject(objectName, getLatestVersionForObject(objectName), cache);
    } else {
      return null;
    }
  }

  @Override
  public void delete(ObjectName objectName) {
    // Delete all suffixes
    list(objectName, true).forEach(object -> {
      String fullName = EhcacheHelper.getReadableObjectName(object.getObjectName()) + getLatestVersionForObject(object.getObjectName());
      LOGGER.debug("Deleting {}", fullName);
      wal.appendUndoAction(new RevertDeleteAction(object.getObjectName(), getLatestVersionForObject(object.getObjectName())));
      byte[] val = deleteFromCache(object.getObjectName());
      tempCache.put(fullName, val);
    });
  }

  public byte[] deleteFromCache(ObjectName objectName) {
    String fullVersion = getLatestVersionForObject(objectName);
    return deleteFromCache(objectName, fullVersion);
  }

  public byte[] deleteFromCache(ObjectName objectName, String fullVersion) {
    LOGGER.debug("Removing {} {}", EhcacheHelper.getReadableObjectName(objectName),
        getLatestVersionForObject(objectName));
    String readableObjectName = EhcacheHelper.getReadableObjectName(objectName);
    List<String> versions = latestVersions.get(readableObjectName);
    if (versions != null) {
      versions.remove(versions.size() - 1);
      if (versions.isEmpty()) {
        latestVersions.remove(readableObjectName);
      }
    }
    byte[] val = cache.get(readableObjectName + fullVersion);
    cache.remove(readableObjectName + fullVersion);
    return val;
  }

  @Override
  public boolean checkObjectExists(ObjectName objectName) {
    return cache.containsKey(EhcacheHelper.getReadableObjectName(objectName) + getLatestVersionForObject(objectName));
  }

  public String getLatestVersionForObject(ObjectName objectName) {
    String readableObjectName = EhcacheHelper.getReadableObjectName(objectName);
    List<String> versions = latestVersions.get(readableObjectName);
    if (versions != null) {
      return versions.get(versions.size() - 1);
    }
    return null;
  }

  public void revertDelete(ObjectName objectName, String version) {
    addToCache(objectName, version, new ByteArrayInputStream(tempCache.get(EhcacheHelper.getReadableObjectName(objectName) + version)));
    tempCache.remove(EhcacheHelper.getReadableObjectName(objectName) + version);
  }

  public void revertAdd(ObjectName objectName) {
    deleteFromCache(objectName);
    tempCache.remove(EhcacheHelper.getReadableObjectName(objectName) + getLatestVersionForObject(objectName));
  }
}
