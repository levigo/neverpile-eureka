package com.neverpile.eureka.impl.tasks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.neverpile.eureka.tasks.TaskQueue;

public class LocalTaskQueue<T> implements TaskQueue<T> {

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
      if (o instanceof LocalTaskQueue.CacheData) {
        CacheData<?> pair = (CacheData<?>) o;
        if (!Objects.equals(info, pair.info))
          return false;
        if (!Objects.equals(state, pair.state))
          return false;
        return true;
      }
      return false;
    }
  }

  private Map<String, CacheData<T>> queueCache = new HashMap<>();

  private final ExecutorService notificationExecutor = Executors.newFixedThreadPool(10);

  private final List<QueueListener<T>> listeners = new ArrayList<>();

  private void notifyListeners() {
    for (QueueListener<T> listener : listeners) {
      notificationExecutor.submit(listener::notifyUpdate);
    }
  }

  @Override
  public void putAllInQueue(final Map<String, T> map) {
    for (Map.Entry<String, T> entry : map.entrySet()) {
      putInQueue(entry.getKey(), entry.getValue());
    }
    notifyListeners();

  }


  @Override
  public void putInQueue(final String key, final T type) {
    queueCache.put(key, new CacheData<>(type, State.OPEN));
    notifyListeners();
  }

  @Override
  public TaskQueue.ProcessElement<T> getElementToProcess() {
    Iterator<Entry<String, CacheData<T>>> queueIterator = queueCache.entrySet().iterator();
    while (queueIterator.hasNext()) {
      Entry<String, CacheData<T>> entry = queueIterator.next();
      String anyKey = entry.getKey();
      CacheData<T> data = entry.getValue();

      switch (data.getState()){
        case OPEN:
          if (queueCache.replace(anyKey, new CacheData<>(data.getInfo(), State.OPEN),
              new CacheData<>(data.getInfo(), State.INPROCESS))) {
            return new TaskQueue.ProcessElement<>(anyKey, data.getInfo());
          }
          break;
        default:
          break;
      }
    }
    return null;
  }

  @Override
  public boolean removeProcessedElement(final String key) {
    if (queueCache.containsKey(key) && State.INPROCESS == queueCache.get(key).getState()) {
      queueCache.remove(key);
      return true;
    }
    return false;
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
