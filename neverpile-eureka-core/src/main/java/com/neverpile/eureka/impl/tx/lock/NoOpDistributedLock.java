package com.neverpile.eureka.impl.tx.lock;

import java.util.concurrent.locks.Lock;

import com.neverpile.eureka.tx.lock.DistributedLock;

public class NoOpDistributedLock implements DistributedLock {

  @Override
  public Lock readLock(final String lockId) {
    return new NoOpLock();
  }

  @Override
  public Lock writeLock(final String lockId) {
    return new NoOpLock();
  }

}
