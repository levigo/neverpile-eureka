package com.neverpile.eureka.tasks;

import java.io.Serializable;
import java.util.Map;

/**
 * This task queue is used to queue in asynchronous Tasks to be executed distributedly. The queue provides access for
 * listeners to get and work on a task. If the task was successfully completed, it will be removed from the queue.
 * The task is parameterized to contain various data with information needed to perform a specific task.
 * This queue must have be injected with unique name by using the {@link DistributedPersistentQueueType} annotation.
 *
 * @param <V> The data type for executing a task.
 */
public interface TaskQueue<V> {

  /**
   * Add a series of tasks to the queue. All tasks are mapped to their unique key.
   *
   * @param map Map containing the unique key and the task data.
   */
  void putAllInQueue(Map<String, V> map);

  /**
   * Append a single task to te queue using a unique task key.
   *
   * @param key a unique task key.
   * @param data the task data.
   */
  void putInQueue(String key, V data);

  /**
   * Get an element from the queue. The returned element has not yet been processed by another node.
   *
   * @return ProcessElement containing the key and the task data.
   */
  ProcessElement<V> getElementToProcess();

  /**
   * Report a task as done and remove it from the queue. Task can only be removed when they have previously been marked as
   * in-progress by getting them via {@link TaskQueue#getElementToProcess()}.
   *
   * @param key key of the task to remove.
   * @return return {@code true} if the removal was successful - {@code false} otherwise.
   */
  boolean removeProcessedElement(String key);

  /**
   * Register a listener on the task queue. The listener will get notified by calling the
   * {@link QueueListener#notifyUpdate()} function. A notification will be triggered whenever an element in the queue is
   * ready for processing.
   * The listener may process a task when notified.
   *
   * @param listener listener to get notified.
   */
  void registerListener(QueueListener<V> listener);

  /**
   * Remove a listener from the task queue to no longer get notifications for queue changes.
   *
   * @param listener listener to remove.
   */
  void unregisterListener(QueueListener<V> listener);

  /**
   * Listener for the task queue to get notifications whenever an element in the queue is ready for processing.
   *
   * @param <T> The data type for the task data.
   */
  interface QueueListener<T> {
    /**
     * This Function will be called whenever an element in the queue is ready for processing.
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
      if (o instanceof ProcessElement<?> element) {
        if (key != null ? !key.equals(element.key) : element.key != null) return false;
        if (value != null ? !value.equals(element.value) : element.value != null) return false;
        return true;
      }
      return false;
    }
  }
}
