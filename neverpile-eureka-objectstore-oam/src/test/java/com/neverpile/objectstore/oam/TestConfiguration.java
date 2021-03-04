package com.neverpile.objectstore.oam;

import java.io.File;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.annotation.RequestScope;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.impl.tx.lock.LocalLockFactory;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;
import com.neverpile.eureka.tx.wal.local.FileBasedWAL;
import com.neverpile.objectstore.oam.OamObjectStoreService;

@Configuration
@EnableAutoConfiguration
@EnableTransactionManagement
public class TestConfiguration {

  @Bean
  ObjectStoreService oamObjectStoreService() {
    return new OamObjectStoreService();
  }

  @Bean
  WriteAheadLog fileBasedWal() {
    new File("./data/tx.log").delete();
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