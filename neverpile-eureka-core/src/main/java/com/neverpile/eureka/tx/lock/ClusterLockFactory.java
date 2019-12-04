package com.neverpile.eureka.tx.lock;

import java.util.concurrent.locks.Lock;

/**
 * A factory interface for cluster-wide locks. Cluster-Lock instances are identified by a lock id.
 */
public interface ClusterLockFactory {

  Lock readLock(String lockId);

  Lock writeLock(String lockId);
}
