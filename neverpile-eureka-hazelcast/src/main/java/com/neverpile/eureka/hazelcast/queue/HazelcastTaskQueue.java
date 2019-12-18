package com.neverpile.eureka.hazelcast.queue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import com.hazelcast.map.listener.MapListener;
import com.neverpile.eureka.tasks.TaskQueue;

import io.micrometer.core.instrument.MeterRegistry;

public class HazelcastTaskQueue<T> implements TaskQueue<T> {
  private final class TaskListener
      implements
        MapListener,
        EntryAddedListener<String, CacheData<T>>,
        EntryUpdatedListener<String, CacheData<T>> {
    @Override
    public void entryUpdated(final EntryEvent<String, CacheData<T>> event) {
      notify(event);
    }

    @Override
    public void entryAdded(final EntryEvent<String, CacheData<T>> event) {
      notify(event);
    }

    private void notify(final EntryEvent<String, CacheData<T>> event) {
      if (event.getValue().getState() == State.OPEN)
        notifyListeners();
    }
  }

  private enum State {
    INPROCESS, OPEN
  }

  private static class CacheData<K> implements Serializable {
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
      if (o instanceof HazelcastTaskQueue.CacheData) {
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

  @Autowired
  private HazelcastInstance hazelcast;

  private IMap<String, CacheData<T>> queueCache;

  @Autowired
  private MeterRegistry meterRegistry;

  private final ExecutorService notificationExecutor = Executors.newFixedThreadPool(10);

  private final List<QueueListener<T>> listeners = new ArrayList<>();

  private final String name;

  public HazelcastTaskQueue(final String name) {
    this.name = name;
  }

  @Scheduled(fixedDelay = 5000)
  public void publishStatistics() {
    meterRegistry.gauge("ignite." + name + ".queue-length", queueCache, Map::size);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + "name='" + name + '\'' + '}';
  }

  @PostConstruct
  private void start() {
    String tasksMapName = getClass().getName() + ".tasks." + name;
    
    queueCache = hazelcast.getMap(tasksMapName);

    queueCache.addEntryListener(new TaskListener(), true);

    if (!queueCache.isEmpty())
      notifyListeners();
  }

  private void notifyListeners() {
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
  public void putInQueue(final String key, final T type) {
    queueCache.put(key, new CacheData<>(type, State.OPEN));
  }

  @Override
  public TaskQueue.ProcessElement<T> getElementToProcess() {
    Iterator<Entry<String, CacheData<T>>> queueIterator = queueCache.entrySet().iterator();
    for (;;) {
      Entry<String, CacheData<T>> entry = queueIterator.hasNext() ? queueIterator.next() : null;
      if (null == entry) {
        return null;
      }
      String anyKey = entry.getKey();
      CacheData<T> data = entry.getValue();

      queueCache.lock(anyKey);
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
        queueCache.unlock(anyKey);
      }
    }
  }

  @Override
  public boolean removeProcessedElement(final String key) {
    queueCache.lock(key);
    try {
      if (queueCache.containsKey(key) && State.INPROCESS == queueCache.get(key).getState()) {
        queueCache.remove(key);
        return true;
      }
      return false;
    } finally {
      queueCache.unlock(key);
    }

  }

  @Override
  public void registerListener(final QueueListener<T> listener) {
    listeners.add(listener);
  }

  @Override
  public void unregisterListener(final QueueListener<T> listener) {
    listeners.remove(listener);
  }
}
