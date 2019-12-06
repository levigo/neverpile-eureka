package com.neverpile.eureka.bridge.storage.s3;

import java.io.File;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.annotation.RequestScope;

import com.amazonaws.Protocol;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.bridge.storage.s3.S3ConnectionConfiguration.AccessStyle;
import com.neverpile.eureka.impl.tx.lock.LocalLockFactory;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;
import com.neverpile.eureka.tx.wal.local.FileBasedWAL;

@Configuration
@EnableAutoConfiguration
@EnableTransactionManagement
public class TestConfiguration {
  @Bean
  ObjectStoreService s3OSS() {
    return new S3ObjectStoreService();
  }

  @Bean
  @Primary
  S3ConnectionConfiguration s3Conf() {
    S3ConnectionConfiguration cc = new S3ConnectionConfiguration();
    cc.setEndpoint("localhost:9876");
    cc.setDefaultBucketName("unit-tests");
    cc.setAccessStyle(AccessStyle.Path);
    cc.setAccessKeyId("foo");
    cc.setSecretAccessKey("bar");
    
    cc.getClientConfiguration()
      .withProtocol(Protocol.HTTP)
      .withTcpKeepAlive(false)
      .withUseExpectContinue(false);
    
    return cc;
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