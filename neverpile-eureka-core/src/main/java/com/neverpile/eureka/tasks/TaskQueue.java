package com.neverpile.eureka.tasks;

import java.io.Serializable;
import java.util.Map;

public interface TaskQueue<V> {

  void putAllInQueue(Map<String, V> map);

  void putInQueue(String key, V type);

  ProcessElement<V> getElementToProcess();

  boolean removeProcessedElement(String key);

  void registerListener(QueueListener<V> listener);

  void unregisterListener(QueueListener<V> listener);

  interface QueueListener<T> {
      void notifyUpdate();
  }

  static class ProcessElement<V> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String key;

    public String getKey() { return key; }

    private final V value;

    public V getValue() { return value; }

    public ProcessElement(final String key, final V value) {
      this.key = key;
      this.value = value;
    }

    public String toString() {
      return key + "=" + value;
    }

    public int hashCode() {
      return key.hashCode() * 13 + (value == null ? 0 : value.hashCode());
    }

    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o instanceof ProcessElement) {
        ProcessElement<?> element = (ProcessElement<?>) o;
        if (key != null ? !key.equals(element.key) : element.key != null) return false;
        if (value != null ? !value.equals(element.value) : element.value != null) return false;
        return true;
      }
      return false;
    }
  }
}
