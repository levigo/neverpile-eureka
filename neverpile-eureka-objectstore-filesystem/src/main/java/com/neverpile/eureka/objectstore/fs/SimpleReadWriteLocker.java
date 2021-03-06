package com.neverpile.eureka.objectstore.fs;

import com.neverpile.eureka.model.ObjectName;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SimpleReadWriteLocker implements ReadWriteLocker {

  private static ConcurrentHashMap<String, ReentrantReadWriteLock> ObjectLocks = new ConcurrentHashMap<>();

  @Override
  public void writeLockObject(ObjectName target) {
    ReentrantReadWriteLock lock = getObjectLock(target);
    lock.writeLock().lock();
  }

  @Override
  public void readLockObject(ObjectName target) {
    ReentrantReadWriteLock lock = getObjectLock(target);
    lock.readLock().lock();
  }

  @Override
  public void writeUnlockObject(ObjectName target) {
    ObjectLocks.compute(target.toString(), (k, v) -> {
      if (v != null) {
        if (!v.hasQueuedThreads()) {
          return null;
        }
        v.writeLock().unlock();
      }
      return v;
    });
  }

  @Override
  public void readUnlockObject(ObjectName target) {
    ObjectLocks.compute(target.toString(), (k, v) -> {
      if (v != null) {
        if (!v.hasQueuedThreads() && v.getReadLockCount() == 1) {
          return null;
        }
        v.readLock().unlock();
      }
      return v;
    });
  }

  private ReentrantReadWriteLock getObjectLock(ObjectName target) {
    return ObjectLocks.compute(target.toString(), (k, v) -> {
      if (v == null) {
        v = new ReentrantReadWriteLock();
      }
      return v;
    });
  }

}
