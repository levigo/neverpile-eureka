package com.neverpile.eureka.hazelcast.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.hazelcast.core.HazelcastInstance;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;

@Component
@Lazy
public class HazelcastLockFactory implements ClusterLockFactory {
  protected static final Logger logger = LoggerFactory.getLogger(HazelcastLockFactory.class);

  @SuppressWarnings("unused")
  @Autowired
  private HazelcastInstance hazelcast;

  @Override
  public Lock readLock(final String lockId) {
    // TODO: implement propper read write Lock
    // return new NeverpileHazelcastLock(lockId, true);
    throw new UnsupportedOperationException();
  }

  @Override
  public Lock writeLock(final String lockId) {
    // TODO: implement propper read write Lock
    // return new NeverpileHazelcastLock(lockId, false);
    throw new UnsupportedOperationException();
  }

  private class NeverpileHazelcastLock implements Lock {
    @SuppressWarnings("unused")
    String lockId;
    boolean isReadLock;

    public NeverpileHazelcastLock(final String lockId, final boolean isReadLock) {
      this.lockId = lockId;
      this.isReadLock = isReadLock;
    }

    @Override
    public void lock() {
      while (!tryLock())
        ;
    }

    @Override
    public boolean tryLock(final long timeout, final TimeUnit unit) {
      long deadline = System.nanoTime() + unit.toNanos(timeout);
      while (!tryLock()) {
        if (System.nanoTime() > deadline) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean tryLock() {
      if (isReadLock) {
        return tryReadLock();
      } else {
        return tryWriteLock();
      }
    }

    private boolean tryWriteLock() {
      throw new UnsupportedOperationException();
    }

    private boolean tryReadLock() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void unlock() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void lockInterruptibly() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Condition newCondition() {
      throw new UnsupportedOperationException();
    }
  }
}