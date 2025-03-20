package com.neverpile.eureka.objectstore.ehcache.simple;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
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

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.objectstore.ehcache.EhcacheConfig;
import com.neverpile.eureka.objectstore.ehcache.EhcacheHelper;

public class EhcacheSimpleObjectStoreService implements ObjectStoreService {

  private static final Logger LOGGER = LoggerFactory.getLogger(EhcacheSimpleObjectStoreService.class);

  private final EhcacheConfig ehcacheConfig;

  private CacheManager cacheManager;
  private Cache<String, byte[]> cache;

  public EhcacheSimpleObjectStoreService(EhcacheConfig ehcacheConfig) {
    this.ehcacheConfig = ehcacheConfig;
  }

  @PostConstruct
  public void init() {
    Path root = FileSystems.getDefault().getPath(ehcacheConfig.getRootPath()).normalize();
    File rootPathAsFile = root.toFile();
    LOGGER.info("-----");
    LOGGER.info("Initializing neverpile eureka - Ehcache Storage Bridge (simple) ...");
    LOGGER.info("Root directory for storing objects: '{}'", root.toAbsolutePath());
    LOGGER.info("Persistence across restarts enabled: '{}'", ehcacheConfig.isPersistent());
    LOGGER.info("configured disk space: {} GB", Integer.parseInt(ehcacheConfig.getDiskSize()) / 1000);
    LOGGER.info("Free space left: {} GB", rootPathAsFile.getFreeSpace() / 1000000000);
    LOGGER.info("Permissions on root directory: {}{}{}", //
        rootPathAsFile.canRead() ? "Read, " : "", //
        rootPathAsFile.canWrite() ? "Write, " : "", //
        rootPathAsFile.canExecute() ? "eXecute" : "");
    LOGGER.info("-----");

    PersistentCacheManager persistentCacheManager = CacheManagerBuilder.newCacheManagerBuilder()
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
                .disk(Integer.parseInt(ehcacheConfig.getDiskSize()), MemoryUnit.MB, ehcacheConfig.isPersistent())
        )
        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(ehcacheConfig.getExpiryTime()))
        .withService(cacheEventListenerConfiguration)
        .build();
    cache = persistentCacheManager.createCache("neverpile-eureka", configuration);
    this.cacheManager = persistentCacheManager;
  }

  @PreDestroy
  void shutdown() {
    this.cacheManager.close();
  }

  @Override
  public void put(ObjectName objectName, String version, InputStream content) {
    addToCache(objectName, version, content);
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
    final String readableObjectName = EhcacheHelper.getReadableObjectName(objectName);
    LOGGER.debug("Adding {}", readableObjectName);
    try {
      cache.put(readableObjectName, content.readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Stream<StoreObject> list(ObjectName prefix) {
    throw new UnsupportedOperationException("Cannot list objects without multi-versioning enabled");
  }

  @Override
  public StoreObject get(ObjectName objectName) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Getting {}", EhcacheHelper.getReadableObjectName(objectName));
    }
    if (checkObjectExists(objectName)) {
      return new EhcacheSimpleStoreObject(objectName, cache);
    } else {
      return null;
    }
  }

  @Override
  public void delete(ObjectName objectName) {
    LOGGER.debug("Deleting {}", objectName);
    deleteFromCache(objectName);
  }

  private void deleteFromCache(ObjectName objectName) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Removing {}", EhcacheHelper.getReadableObjectName(objectName));
    }
    String readableObjectName = EhcacheHelper.getReadableObjectName(objectName);
    cache.remove(readableObjectName);
  }

  @Override
  public boolean checkObjectExists(ObjectName objectName) {
    return cache.containsKey(EhcacheHelper.getReadableObjectName(objectName));
  }

}
