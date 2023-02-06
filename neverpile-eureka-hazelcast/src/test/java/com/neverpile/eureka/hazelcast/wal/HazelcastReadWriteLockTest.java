package com.neverpile.eureka.hazelcast.wal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.eureka.tx.lock.ClusterLockFactory;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class, properties = {
    "neverpile.wal.ignite.auto-rollback-timeout=1", "neverpile.wal.ignite.prune-interval=1000"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class HazelcastReadWriteLockTest {
  @Autowired
  ClusterLockFactory rwlf;

  AtomicInteger count;

  @Test
  public void testThat_writersBlockWriters() throws InterruptedException {
    count = new AtomicInteger(0);
    Thread t1 = new Thread(() -> {
      Lock lock = rwlf.writeLock("test1");
      lock.lock();
      try {
        count.incrementAndGet();
      } finally {
        lock.unlock();
      }
    });

    Lock lock = rwlf.writeLock("test1");
    lock.lock();
    try {
      count.incrementAndGet();
      t1.start();
      Thread.sleep(1000);

      Assert.assertEquals(1, count.get());
    } finally {
      lock.unlock();
    }
    t1.join();
    Assert.assertEquals(2, count.get());
  }


  // the @HazelcastSimpleLockFactory implementation is a simple lock without any read/write functionality.
  // TODO: has to be reenabled, when the rw-lock implementation is done.
  @Test
  @Ignore
  public void testThat_multipleReadersCanAccess() throws InterruptedException {
    count = new AtomicInteger(0);
    Thread t1 = new Thread(() -> {
      Lock lock = rwlf.readLock("test2");
      lock.lock();
      try {
        count.incrementAndGet();
      } finally {
        lock.unlock();
      }
    });

    Lock lock1 = rwlf.readLock("test2");
    lock1.lock();
    try {
      count.incrementAndGet();

      t1.start();
      Thread.sleep(1000);
      Assert.assertEquals(2, count.get());
    } finally {
      lock1.unlock();
    }

    Assert.assertEquals(2, count.get());
  }

  @Test
  public void testThat_readersBlockWriters() throws InterruptedException {
    count = new AtomicInteger(0);

    count = new AtomicInteger(0);
    Thread t1 = new Thread(() -> {
      Lock lock = rwlf.writeLock("test3");
      lock.lock();
      try {
        count.incrementAndGet();
      } finally {
        lock.unlock();
      }
    });

    Lock lock1 = rwlf.readLock("test3");
    lock1.lock();
    try {
      count.incrementAndGet();
      t1.start();
      Thread.sleep(1000);
      Assert.assertEquals(1, count.get());
    } finally {
      lock1.unlock();
    }
    t1.join();
    Assert.assertEquals(2, count.get());
  }

  @Test
  public void testThat_writersBlockReaders() throws InterruptedException {
    count = new AtomicInteger(0);

    count = new AtomicInteger(0);
    Thread t1 = new Thread(() -> {
      Lock lock = rwlf.readLock("test4");
      lock.lock();
      try {
        count.incrementAndGet();
      } finally {
        lock.unlock();
      }
    });

    Lock lock1 = rwlf.writeLock("test4");
    lock1.lock();
    try {
      count.incrementAndGet();
      t1.start();
      Thread.sleep(1000);
      Assert.assertEquals(1, count.get());
    } finally {
      lock1.unlock();
    }
    t1.join();
    Assert.assertEquals(2, count.get());
  }

  private void readerTryRun() {
    Lock lock = rwlf.readLock("test5");
    if (lock.tryLock()) {
      try {
        count.incrementAndGet();
      } finally {
        lock.unlock();
      }
    } else {
      count.decrementAndGet();
    }
  }

  @Test
  public void testThat_tryLockWorksForReaders() throws InterruptedException {
    count = new AtomicInteger(0);

    Thread t1 = new Thread(() -> readerTryRun());

    t1.start();
    t1.join();
    Assert.assertEquals(1, count.get());

    t1 = new Thread(() -> readerTryRun());

    Lock lock1 = rwlf.writeLock("test5");
    lock1.lock();
    try {
      t1.start();
      t1.join();
      Assert.assertEquals(0, count.get());
    } finally {
      lock1.unlock();
    }
    Assert.assertEquals(0, count.get());
  }

  private void writersTryRun() {
    Lock lock = rwlf.writeLock("test6");
    if (lock.tryLock()) {
      try {
        count.incrementAndGet();
      } finally {
        lock.unlock();
      }
    } else {
      count.decrementAndGet();
    }
  }

  @Test
  public void testThat_tryLockWorksForWriters() throws InterruptedException {
    count = new AtomicInteger(0);

    Thread t1 = new Thread(() -> writersTryRun());

    t1.start();
    t1.join();
    Assert.assertEquals(1, count.get());

    t1 = new Thread(() -> writersTryRun());

    Lock lock1 = rwlf.writeLock("test6");
    lock1.lock();
    try {
      t1.start();
      t1.join();
      Assert.assertEquals(0, count.get());
    } finally {
      lock1.unlock();
    }
    Assert.assertEquals(0, count.get());
  }

  @Test
  public void testThat_tryLockWorksForReadersWithTimeout() throws InterruptedException {
    count = new AtomicInteger(0);

    Thread t1 = new Thread(() -> {
      Lock lock = rwlf.readLock("test7");
      try {
        if (lock.tryLock(10, TimeUnit.SECONDS)) {
          try {
            count.incrementAndGet();
          } finally {
            lock.unlock();
          }
        } else {
          count.decrementAndGet();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    Lock lock1 = rwlf.writeLock("test7");
    lock1.lock();
    try {
      t1.start();
      Thread.sleep(500);
      Assert.assertEquals(0, count.get());
      Thread.sleep(500);
    } finally {
      lock1.unlock();
    }
    t1.join();
    Assert.assertEquals(1, count.get());
  }

  @Test
  public void testThat_tryLockWorksForWritersWithTimeout() throws InterruptedException {
    count = new AtomicInteger(0);

    Thread t1 = new Thread(() -> {
      Lock lock = rwlf.writeLock("test8");
      try {
        if (lock.tryLock(10, TimeUnit.SECONDS)) {
          try {
            count.incrementAndGet();
          } finally {
            lock.unlock();
          }
        } else {
          count.decrementAndGet();
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });

    Lock lock1 = rwlf.writeLock("test8");
    lock1.lock();
    try {
      t1.start();
      Thread.sleep(500);
      Assert.assertEquals(0, count.get());
      Thread.sleep(500);
    } finally {
      lock1.unlock();
    }
    t1.join();
    Assert.assertEquals(1, count.get());
  }

}
