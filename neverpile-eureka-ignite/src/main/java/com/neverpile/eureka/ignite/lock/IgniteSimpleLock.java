package com.neverpile.eureka.ignite.lock;

import java.util.concurrent.locks.Lock;

import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.neverpile.eureka.tx.lock.DistributedLock;

public class IgniteSimpleLock implements DistributedLock {
  protected static final Logger logger = LoggerFactory.getLogger(IgniteSimpleLock.class);

  @Autowired
  private Ignite ignite;

  @Override
  public Lock readLock(final String lockId) {
    return ignite.reentrantLock(lockId, true, true, true);
  }

  @Override
  public Lock writeLock(final String lockId) {
    return ignite.reentrantLock(lockId, true, true, true);
  }
}
