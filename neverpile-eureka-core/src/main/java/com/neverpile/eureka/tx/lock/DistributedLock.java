package com.neverpile.eureka.tx.lock;

import java.util.concurrent.locks.Lock;

public interface DistributedLock {

  Lock readLock(String lockId);

  Lock writeLock(String lockId);
}
