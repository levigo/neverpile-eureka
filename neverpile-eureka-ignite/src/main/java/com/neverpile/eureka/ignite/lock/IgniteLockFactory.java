package com.neverpile.eureka.ignite.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.neverpile.eureka.tx.lock.ClusterLockFactory;

// TODO: implement proper read write lock
// https://issues.apache.org/jira/browse/IGNITE-9
public class IgniteLockFactory implements ClusterLockFactory {
  protected static final Logger logger = LoggerFactory.getLogger(IgniteLockFactory.class);

  @Autowired
  private Ignite ignite;

  @Override
  public Lock readLock(final String lockId) {
    return new neverpileIgniteLock(lockId, true);
  }

  @Override
  public Lock writeLock(final String lockId) {
    return new neverpileIgniteLock(lockId, false);
  }

  private class neverpileIgniteLock implements Lock {
    String lockId;
    boolean isReadLock;

    public neverpileIgniteLock(final String lockId, final boolean isReadLock) {
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
      boolean result = false;
      ignite.reentrantLock(lockId + "-check", true, true, true).lock();
      try {
        IgniteAtomicLong readers = ignite.atomicLong(lockId + "-readers", 0, true);
        if (readers.get() == 0) {
          result = ignite.reentrantLock(lockId + "-write", true, true, true).tryLock();
        }
      } finally {
        ignite.reentrantLock(lockId + "-check", true, true, true).unlock();
      }
      return result;
    }

    private boolean tryReadLock() {
      boolean result = false;
      ignite.reentrantLock(lockId + "-check", true, true, true).lock();
      try {
        IgniteAtomicLong readers = ignite.atomicLong(lockId + "-readers", 0, true);
        if (readers.get() > 0) {
          readers.incrementAndGet();
          result = true;
        } else {
          if (ignite.reentrantLock(lockId + "-write", true, true, true).tryLock()) {
            try {
              readers.incrementAndGet();
            } finally {
              ignite.reentrantLock(lockId + "-write", true, true, true).unlock();
            }
            result = true;
          }
        }
      } finally {
        ignite.reentrantLock(lockId + "-check", true, true, true).unlock();
      }
      return result;
    }

    @Override
    public void unlock() {
      if (isReadLock) {
        IgniteAtomicLong readers = ignite.atomicLong(lockId + "-readers", 0, true);
        readers.decrementAndGet();
      } else {
        ignite.reentrantLock(lockId + "-write", true, true, true).unlock();
      }
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