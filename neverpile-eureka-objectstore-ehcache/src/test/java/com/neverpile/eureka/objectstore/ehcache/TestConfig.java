package com.neverpile.eureka.objectstore.ehcache;

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
  public ObjectStoreService ehcacheObjectStoreService() {
    return new EhcacheObjectStoreService("./target/testdir", "1000", "100", "1");
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