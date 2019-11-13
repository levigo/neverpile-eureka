package com.neverpile.eureka.hazelcast.wal;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.annotation.RequestScope;

import com.neverpile.eureka.hazelcast.lock.HazelcastReadWriteLock;
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

  @Bean
  HazelcastReadWriteLock igniteReadWriteLock() {
    return new HazelcastReadWriteLock();
  }
}