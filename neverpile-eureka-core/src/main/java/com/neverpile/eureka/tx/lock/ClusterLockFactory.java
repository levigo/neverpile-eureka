package com.neverpile.eureka.tx.lock;

import java.util.concurrent.locks.Lock;

/**
 * A factory interface for cluster-wide locks. Cluster-Lock instances are differentiated and identified by a unique
 * lock id.
 */
public interface ClusterLockFactory {

  /**
   * Gets or creates a lock used for reading with a unique, custer wide ID.
   *
   * @param lockId the ID for the lock.
   *
   * @return the lock used for reading
   */
  Lock readLock(String lockId);

  /**
   * Gets or creates a lock used for writing with a unique, custer wide ID..
   *
   * @param lockId the ID for the lock.
   *
   * @return the lock used for writing
   */
  Lock writeLock(String lockId);
}
