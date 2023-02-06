package com.neverpile.eureka.hazelcast.wal;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.annotation.RequestScope;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.CPSubsystem;
import com.neverpile.eureka.hazelcast.lock.HazelcastSimpleLockFactory;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;

@Configuration
@EnableTransactionManagement
@EnableAutoConfiguration
@EnableScheduling
@Import(com.neverpile.eureka.hazelcast.TestConfig.class)
public class TestConfig {
  @Bean
  WriteAheadLog wal() {
    return new HazelcastWAL();
  }

  @Bean
  @RequestScope
  TransactionWAL txwal() {
    return new DefaultTransactionWAL();
  }

  // TODO: has to be repaced with a propper read-write lock wehn implementation is done.
  @Bean
  HazelcastSimpleLockFactory hazelcastReadWriteLock() {
    return new HazelcastSimpleLockFactory();
  }

  @Bean
  HazelcastInstance hazelcast() {
    return Hazelcast.newHazelcastInstance();
  }

  @Bean
  CPSubsystem cpSubsystem(HazelcastInstance hazelcastInstance) {
    return hazelcastInstance.getCPSubsystem();
  }
}