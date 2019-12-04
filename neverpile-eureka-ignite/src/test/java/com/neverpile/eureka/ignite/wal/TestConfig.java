package com.neverpile.eureka.ignite.wal;

import com.neverpile.eureka.ignite.lock.IgniteLockFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.annotation.RequestScope;

import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;

@Configuration
@EnableTransactionManagement
@EnableAutoConfiguration
@EnableScheduling
@Import(com.neverpile.eureka.ignite.TestConfig.class)
public class TestConfig {
  @Bean
  WriteAheadLog wal() {
    return new IgniteWAL();
  }

  @Bean
  @RequestScope
  TransactionWAL txwal() {
    return new DefaultTransactionWAL();
  }

  @Bean
  IgniteLockFactory igniteReadWriteLock() {
    return new IgniteLockFactory();
  }
}