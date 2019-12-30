package com.neverpile.eureka.tx.atomic;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This Interface defines a distributed variant of {@link AtomicReference}. Operations on the Object Referenced will be
 * executed atomically and the changes will be propagated to all nodes in the cluster. This Reference must have a
 * injected unique name by using the {@link DistributedAtomicType} annotation.
 *
 * @param <E> Type of the object to reference.
 */
public interface DistributedAtomicReference<E extends Serializable> {
  /**
   * Function object to be executed atomically in the cluster and alter the referenced object depending on the
   * current state.
   *
   * @param <E> Type of the object to reference.
   */
  interface ReferenceFunction<E> extends Serializable {
    /**
     * Function to alter the referenced object.
     *
     * @param input the referenced object to alter in its state before modification.
     * @return the updated object.
     */
    E apply(E input);
  }

  /**
   * Compare the referenced object with and expected state and if the state matches the expectation alter it atomically.
   *
   * @param expect the expected object state before modification
   * @param update the new state of the object to be set.
   * @return {@code true} if the expected state matches and the object has been updated - {@code false} otherwise.
   */
  boolean compareAndSet(E expect, E update);

  /**
   * Gets the reference in its current state.
   *
   * @return the object in its current state.
   */
  E get();

  /**
   * Set a new state of the reference regardless of its previous state.
   * It is recommended to use {@link DistributedAtomicReference#compareAndSet(Serializable, Serializable)} if the
   * previous state is known to prevent lost updates.
   *
   * @param newValue the object in its updated state.
   */
  void set(E newValue);

  /**
   * Execute a Atomic Function on the Object the Function will get the referenced Object as input and returns the
   * object in its updated state.
   *
   * @param fn function to be executed.
   * @return the object in its updated state.
   */
  E alterAndGet(ReferenceFunction<E> fn);
}
