package com.neverpile.eureka.ignite.queue;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.EventType;
import javax.cache.expiry.EternalExpiryPolicy;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.neverpile.eureka.ignite.IgniteConfigurationProperties;
import com.neverpile.eureka.tasks.TaskQueue;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;

public class IgniteTaskQueue<T> implements TaskQueue<T> {

  private static final Logger logger = LoggerFactory.getLogger(IgniteTaskQueue.class);
  
  @Autowired
  private Ignite ignite;

  @Autowired
  private IgniteConfigurationProperties config;

  private IgniteCache<String, CacheData<T>> queueCache;

  @Autowired
  MeterRegistry meterRegistry;
  
  private final ExecutorService notificationExecutor = Executors.newFixedThreadPool(10);

  private final List<QueueListener<T>> listeners = new CopyOnWriteArrayList<>();

  private final String name;

  private QueryCursor<Cache.Entry<String, CacheData<T>>> continuousQuery;

  private static final class RemoteEventFilter<T> implements CacheEntryEventFilter<String, CacheData<T>> {
    @Override
    public boolean evaluate(final CacheEntryEvent<? extends String, ? extends CacheData<T>> event)
        throws CacheEntryListenerException {
      return (event.getEventType() == EventType.CREATED || event.getEventType() == EventType.UPDATED)
          && event.getValue().getState() == State.OPEN;
    }
  }

  private enum State {
    INPROCESS, OPEN
  }

  static class CacheData<K> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final K info;

    K getInfo() {
      return info;
    }

    private final State state;

    State getState() {
      return state;
    }

    public CacheData(final K key, final State value) {
      this.info = key;
      this.state = value;
    }

    @Override
    public String toString() {
      return info + "=" + state;
    }

    @Override
    public int hashCode() {
      return info.hashCode() * 13 + (state == null ? 0 : state.hashCode());
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o)
        return true;
      if (o instanceof IgniteTaskQueue.CacheData) {
        CacheData<?> pair = (CacheData<?>) o;
        if (info != null ? !info.equals(pair.info) : pair.info != null)
          return false;
        if (state != null ? !state.equals(pair.state) : pair.state != null)
          return false;
        return true;
      }
      return false;
    }
  }

  public IgniteTaskQueue(final String name) {
    this.name = name;
  }

  @PostConstruct
  public void init() {
    meterRegistry.gauge("ignite." + name + ".queue-length", queueCache, IgniteCache::size);
  }
  
  @Override
  public String toString() {
    return "IgniteProcessQueueCache{" + "name='" + name + '\'' + '}';
  }

  @PostConstruct
  private void start() {
    CacheConfiguration<String, CacheData<T>> ccTx = new CacheConfiguration<>(name);
    ccTx.setBackups(1);
    ccTx.setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());
    ccTx.setCacheMode(CacheMode.REPLICATED);

    if (config.getPersistence().isEnabled())
      ccTx.setDataRegionName("persistent");

    ccTx.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
    queueCache = ignite.getOrCreateCache(ccTx);

    ContinuousQuery<String, CacheData<T>> qry = new ContinuousQuery<>();

    qry.setInitialQuery(new ScanQuery<String, CacheData<T>>((k, v) -> v.state == State.OPEN));

    qry.setLocalListener((evts) -> evts.forEach(e -> {
      if (e.getEventType() == EventType.CREATED || e.getEventType() == EventType.UPDATED)
        notifyListeners(e.getKey(), e.getValue());
    }));

    qry.setRemoteFilterFactory(() -> new RemoteEventFilter<T>());

    continuousQuery = queueCache.query(qry);

    // deal with initial results
    continuousQuery.forEach(e -> notifyListeners(e.getKey(), e.getValue()));
  }

  private void notifyListeners(final String key, final CacheData<T> value) {
    for (QueueListener<T> listener : listeners) {
      notificationExecutor.submit(() -> listener.notifyUpdate());
    }
  }

  @Override
  public void putAllInQueue(final Map<String, T> map) {
    for (Map.Entry<String, T> entry : map.entrySet()) {
      putInQueue(entry.getKey(), entry.getValue());
    }
  }


  @Override
  @Timed(description = "put process queue element", extraTags = {"subsystem", "ignite.process-queue"}, value="eureka.ignite.process-queue.put")
  public void putInQueue(final String key, final T type) {
    queueCache.put(key, new CacheData<>(type, State.OPEN));
  }

  @Override
  @Timed(description = "get process queue element", extraTags = {"subsystem", "ignite.process-queue"}, value="eureka.ignite.process-queue.get")
  public TaskQueue.ProcessElement<T> getElementToProcess() {
    Iterator<Cache.Entry<String, CacheData<T>>> queueIterator = queueCache.iterator();
    for (;;) {
      Cache.Entry<String, CacheData<T>> entry = queueIterator.hasNext() ? queueIterator.next() : null;
      if (null == entry) {
        return null;
      }
      String anyKey = entry.getKey();
      CacheData<T> data = entry.getValue();

      Lock lock = queueCache.lock(anyKey);
      if (!lock.tryLock()) {
        continue;
      }
      try {
        switch (data.getState()){
          case OPEN :
            if (queueCache.replace(anyKey, new CacheData<>(data.getInfo(), State.OPEN),
                new CacheData<>(data.getInfo(), State.INPROCESS))) {
              return new TaskQueue.ProcessElement<>(anyKey, data.getInfo());
            }
            break;
          default :
            break;
        }
      } finally {
        lock.unlock();
      }
    }
  }

  @Override
  @Timed(description = "remove process queue element", extraTags = {"subsystem", "ignite.process-queue"}, value="eureka.ignite.process-queue.remove")
  public boolean removeProcessedElement(final String key) {
    Lock lock = queueCache.lock(key);
    lock.lock();
    try {
      if (queueCache.containsKey(key) && State.INPROCESS == queueCache.get(key).getState()) {
        queueCache.remove(key);
        return true;
      }
      return false;
    } finally {
      lock.unlock();
    }

  }

  @Override
  public void registerListener(final QueueListener<T> listener) {
    listeners.add(listener);
  }

  @Override
  public void unregisterListener(final QueueListener<T> listener) {
    if(!listeners.remove(listener))
      logger.warn("Failed to remove a listener");
  }
}
