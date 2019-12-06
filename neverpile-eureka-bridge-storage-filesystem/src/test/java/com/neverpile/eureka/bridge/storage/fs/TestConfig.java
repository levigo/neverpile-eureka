package com.neverpile.eureka.bridge.storage.fs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.impl.tx.lock.LocalLockFactory;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;
import com.neverpile.eureka.tx.wal.local.FileBasedWAL;

@Configuration
public class TestConfig {
  @Bean
  public ObjectStoreService fsObjectStoreService() {
    return new FilesystemObjectStoreService();
  }

  @Bean
  WriteAheadLog fileBasedWal() {
    return new FileBasedWAL();
  }

  @Bean
  @RequestScope
  TransactionWAL wal() {
    return new DefaultTransactionWAL();
  }
  
  @Bean
  ClusterLockFactory lock() {
    return new LocalLockFactory();
  }
}