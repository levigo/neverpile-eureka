package com.neverpile.eureka.impl.tx.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class NoOpLock implements Lock {

  @Override
  public void lock() {
    // do nothing
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    // do nothing
  }

  @Override
  public boolean tryLock() {
    return true; // always succeed
  }

  @Override
  public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
    return true; // always succeed
  }

  @Override
  public void unlock() {
    // do nothing
  }

  @Override
  public Condition newCondition() {
    throw new UnsupportedOperationException("Creating conditions is not supported by this type of lock");
  }
}
