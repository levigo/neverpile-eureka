package com.neverpile.eureka.impl.tx.lock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

import com.neverpile.eureka.impl.tx.lock.LocalLockFactory.ReadWriteLockWeakReference.LockWrapper;

import org.junit.jupiter.api.Test;

public class LocalLockFactoryTest {
  @Test
  public void testThat_factoryReturnsLocksByKey() {
    LocalLockFactory f = new LocalLockFactory();

    LockWrapper rl = (LockWrapper) f.readLock("foo"); // must keep hard ref to prevent collection
    assertThat(rl.delegate).isSameAs(((LockWrapper) f.readLock("foo")).delegate);

    LockWrapper wl = (LockWrapper) f.writeLock("foo"); // must keep hard ref to prevent collection
    assertThat(wl.delegate).isSameAs(((LockWrapper) f.writeLock("foo")).delegate);

    assertThat(rl.delegate).isNotSameAs(((LockWrapper) f.readLock("bar")).delegate);
    assertThat(wl.delegate).isNotSameAs(((LockWrapper) f.writeLock("bar")).delegate);
  }

  @Test
  public void testThat_locksCanBeRetrievedAgainAfterCollection() throws InterruptedException {
    LocalLockFactory f = new LocalLockFactory();
    
    // fetch lock but forget ref immediately
    f.readLock("foo");
    
    // force gc and thus ref enqueueing
    Thread.sleep(100);
    System.gc();
    
    // new instance of lock must be retrievable
    f.readLock("foo");
  }
  
  @Test
  public void testThat_lockCollectionWorks() throws InterruptedException {
    LocalLockFactory f = new LocalLockFactory();

    // lock/unlock to soften ref
    Lock rl = f.readLock("foo");
    rl.lock();
    rl.unlock();

    Lock wl = f.readLock("foo");
    wl.lock();
    wl.unlock();

    // keep delegate...
    Lock d = ((LockWrapper) rl).delegate;
    rl = null;
    wl = null;

    // This may be unreliable, but well...
    for (int i = 0; i < 100; i++) {
      System.gc();
      Thread.sleep(10);

      if (((LockWrapper) f.readLock("foo")).delegate != d)
        break;
    }

    // Caveat: is(Not)EqualTo relies on a special property of the created lock implementations
    // to indicate equality upon identical delegate lock references.
    assertThat(d).isNotEqualTo(((LockWrapper) f.readLock("foo")).delegate);
  }

  @Test
  public void testThat_locksWorkAsExpected() throws Exception {
    ExecutorService p = Executors.newFixedThreadPool(1);
    LocalLockFactory f = new LocalLockFactory();

    Lock readLock = f.readLock("foo");
    Lock writeLock = f.writeLock("foo");

    // read does not block read
    readLock.lock();
    assertThat(p.submit(() -> {
      try {
        return readLock.tryLock();
      } finally {
        readLock.unlock();
      }
    }).get()).isTrue();
    readLock.unlock();

    // read blocks writes
    readLock.lock();
    assertThat(p.submit(() -> writeLock.tryLock()).get()).isFalse();
    readLock.unlock();

    // write blocks read
    writeLock.lock();
    assertThat(p.submit(() -> readLock.tryLock()).get()).isFalse();
    writeLock.unlock();

    p.shutdown();
  }
}
