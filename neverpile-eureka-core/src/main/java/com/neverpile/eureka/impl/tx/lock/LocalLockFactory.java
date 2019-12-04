package com.neverpile.eureka.impl.tx.lock;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.annotations.VisibleForTesting;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;
import com.neverpile.eureka.util.Threads;

/**
 * A factory for creating local, non-cluster-wide locks. Only good for use in single-instance
 * setups.
 */
public class LocalLockFactory implements ClusterLockFactory {

  private final Map<String, ReadWriteLockWeakReference> locks = new ConcurrentHashMap<>();

  private final ReferenceQueue<ReadWriteLock> disusedLockQueue = new ReferenceQueue<>();

  /**
   * A {@link ReadWriteLock} wrapper where the {@link ReadWriteLock} is held weakly as long as no
   * locks are actually held. This lets us get rid of the locks as soon as they are no longer in
   * use.
   */
  @VisibleForTesting
  final class ReadWriteLockWeakReference extends WeakReference<ReadWriteLock> implements ReadWriteLock {
    @VisibleForTesting
    class LockWrapper implements Lock {
      @VisibleForTesting
      final java.util.concurrent.locks.Lock delegate;

      // hard back-ref so that holding on to this instance also holds the parent
      @SuppressWarnings("unused")
      private final ReadWriteLock parentRef;

      public LockWrapper(final ReadWriteLock parentRef, final Lock delegate) {
        this.parentRef = parentRef;
        this.delegate = delegate;
      }

      public void lock() {
        delegate.lock();
        incrementHoldCount();
      }

      public void lockInterruptibly() throws InterruptedException {
        delegate.lockInterruptibly();
        incrementHoldCount();
      }

      public boolean tryLock() {
        boolean success = delegate.tryLock();
        if (success)
          incrementHoldCount();
        return success;
      }

      public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
        return delegate.tryLock(time, unit);
      }

      public void unlock() {
        delegate.unlock();
        decrementHoldCount();
      }

      public Condition newCondition() {
        return delegate.newCondition();
      }
    }

    String key;

    // this hard ref is used while in hard ref mode
    @SuppressWarnings("unused")
    private ReadWriteLock hardRef;

    private final AtomicInteger holdCounter = new AtomicInteger();

    public ReadWriteLockWeakReference(final String key, final ReadWriteLock lock) {
      super(lock, disusedLockQueue);
      this.hardRef = lock;
      this.key = key;
    }

    private void incrementHoldCount() {
      harden();
      holdCounter.incrementAndGet();
    }

    private void decrementHoldCount() {
      if (holdCounter.decrementAndGet() <= 0)
        soften();
    }

    private void soften() {
      hardRef = null;
    }

    private void harden() {
      hardRef = get();
    }

    @Override
    public java.util.concurrent.locks.Lock readLock() {
      try {
        ReadWriteLock rwl = get();
        if (null == rwl)
          throw new IllegalStateException("Already collected");
        return new LockWrapper(rwl, rwl.readLock());
      } finally {
        soften();
      }
    }

    @Override
    public java.util.concurrent.locks.Lock writeLock() {
      try {
        ReadWriteLock rwl = get();
        if (null == rwl)
          throw new IllegalStateException("Already collected");
        return new LockWrapper(rwl, rwl.writeLock());
      } finally {
        soften();
      }
    }
  }

  private ReadWriteLockWeakReference getReadWriteLock(final String lockId) {
    ReadWriteLock hardRef = null;
    do {
      prune();

      // If there is an existing lock instance for this key, but it has already
      // been collected, poll the reference queue until the situation has been cleared.
      ReadWriteLockWeakReference ref = locks.get(lockId);
      
      // Hold an additional hard reference while we're doing our checks
      if(null != ref)
        hardRef = ref.get();
      
      // If there is no ref or there is one, but it hasn't been cleared, we're good to go
      if (ref == null || hardRef != null)
        break;

      // Sleep 1ms before retrying to reduce churn
      Threads.sleepSafely(1);
    } while (true);

    return locks.computeIfAbsent(lockId, k -> new ReadWriteLockWeakReference(k, new ReentrantReadWriteLock()));
  }

  private void prune() {
    Reference<? extends ReadWriteLock> ref;
    while ((ref = disusedLockQueue.poll()) != null) {
      locks.remove(((ReadWriteLockWeakReference) ref).key);
    }
  }

  @Override
  public Lock readLock(final String lockId) {
    return getReadWriteLock(lockId).readLock();
  }

  @Override
  public Lock writeLock(final String lockId) {
    return getReadWriteLock(lockId).writeLock();
  }

}
