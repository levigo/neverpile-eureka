package com.neverpile.eureka.tasks;

import java.io.Serializable;
import java.util.Map;

/**
 * this task queue is used to queue in asynchronous Tasks to be executed distributedly. The queue provides access for
 * listeners to get and work on a task. if the task was successfully completed it will be removed from the queue.
 * The Task is Parameterized to contain various data with information needed to perform a specific task.
 * This queue must have a injected unique name by using the {@link DistributedPersistentQueueType} annotation.
 *
 * @param <V> The data type for executing a task.
 */
public interface TaskQueue<V> {

  /**
   * Add a Series of Tasks to the Queue. All Tasks are mapped to their unique key.
   *
   * @param map Map containing the unique key and the task data.
   */
  void putAllInQueue(Map<String, V> map);

  /**
   * Append a single Task to te Queue using a unique task key.
   *
   * @param key a unique task key.
   * @param data the task data.
   */
  void putInQueue(String key, V data);

  /**
   * get a element from the queue. The returned element has not jet been processed by another node.
   *
   * @return ProcessElement containing the key and the task data.
   */
  ProcessElement<V> getElementToProcess();

  /**
   * Report a Task as Done and remove it from the queue. Task can only be removed when they are previously marked as
   * in-progress by getting them via {@link TaskQueue#getElementToProcess()}.
   *
   * @param key key of the Task to remove.
   * @return return {@code true} if the removal was successful - {@code false} otherwise.
   */
  boolean removeProcessedElement(String key);

  /**
   * Register a listener on the task queue. the listener will get notified by calling the
   * {@link QueueListener#notifyUpdate()} function. A notification will be triggered whenever a element in the queue is
   * ready for processing.
   * The listener may process a task when notified.
   *
   * @param listener listener to get notified.
   */
  void registerListener(QueueListener<V> listener);

  /**
   * remove a listener from the task queue to no longer get notifications for queue changes.
   *
   * @param listener listener to remove.
   */
  void unregisterListener(QueueListener<V> listener);

  /**
   * Listener for the Task queue to get notifications  whenever a element in the queue is ready for processing.
   *
   * @param <T> The data type for the task data.
   */
  interface QueueListener<T> {
    /**
     * This Function will be called whenever a element in the queue is ready for processing.
     */
      void notifyUpdate();
  }

  /**
   * Wrapper object to bundle the task ID and the mapped task data.
   *
   * @param <V> The data type for the task data.
   */
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
