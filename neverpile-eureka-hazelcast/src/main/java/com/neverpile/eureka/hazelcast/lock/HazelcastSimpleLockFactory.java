package com.neverpile.eureka.hazelcast.lock;

import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.CPSubsystem;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;

public class HazelcastSimpleLockFactory implements ClusterLockFactory {
  protected static final Logger logger = LoggerFactory.getLogger(HazelcastSimpleLockFactory.class);

  @Autowired
  private HazelcastInstance hazelcast;

  @Autowired
  private CPSubsystem cpSubsystem;

  @Override
  public Lock readLock(final String lockId) {
    return cpSubsystem.getLock(lockId);
  }

  @Override
  public Lock writeLock(final String lockId) {
    return cpSubsystem.getLock(lockId);
  }
}
