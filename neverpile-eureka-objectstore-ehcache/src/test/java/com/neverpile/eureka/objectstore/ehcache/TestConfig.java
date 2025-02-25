package com.neverpile.eureka.objectstore.ehcache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.annotation.RequestScope;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.impl.tx.lock.LocalLockFactory;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;

@Configuration
@Import(EhcacheConfig.class)
public class TestConfig {

  @Bean
  public ObjectStoreService ehcacheObjectStoreService() {
    return new EhcacheObjectStoreService();
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