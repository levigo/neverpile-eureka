package com.neverpile.eureka.ignite.cachemanager;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;

/**
 * An adapter from {@link Ignite} and {@link IgniteCache} to spring's {@link CacheManager} and
 * {@link Cache}.
 */
public class IgniteCacheManager implements CacheManager {
  private static class IgniteSpringCache implements Cache {
    private final IgniteCache<Object, Object> cache;

    public IgniteSpringCache(final IgniteCache<Object, Object> cache) {
      this.cache = cache;
    }

    @Override
    public String getName() {
      return cache.getName();
    }

    @Override
    public Object getNativeCache() {
      return cache;
    }

    @Override
    public ValueWrapper get(final Object key) {
      return new SimpleValueWrapper(cache.get(key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Object key, final Class<T> type) {
      Object v = cache.get(key);
      if (null == v)
        return null;

      if (!type.isAssignableFrom(v.getClass()))
        throw new IllegalStateException("Cache entry not of the expected type");

      return (T) v;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Object key, final Callable<T> valueLoader) {
      Object v = cache.get(key);
      if (null == v)
        try {
          return (T) cache.getAndPutIfAbsent(key, valueLoader.call());
        } catch (Exception e) {
          throw new ValueRetrievalException(key, valueLoader, e);
        }

      return (T) v;
    }

    @Override
    public void put(final Object key, final Object value) {
      cache.put(key, value);
    }

    @Override
    public void evict(final Object key) {
      cache.remove(key);
    }

    @Override
    public void clear() {
      cache.clear();
    }
  }

  @Autowired
  Ignite ignite;

  @Override
  public Cache getCache(final String name) {
    // IgniteSpringCache is not cached but re-created on every getCache, but it is just a very cheap
    // facade.
    return new IgniteSpringCache(ignite.getOrCreateCache(name));
  }

  @Override
  public Collection<String> getCacheNames() {
    return ignite.cacheNames();
  }
}
