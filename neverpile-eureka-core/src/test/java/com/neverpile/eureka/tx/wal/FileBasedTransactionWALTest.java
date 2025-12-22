package com.neverpile.eureka.tx.wal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.annotation.RequestScope;

import com.neverpile.eureka.api.BaseTestConfiguration;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;
import com.neverpile.eureka.tx.wal.local.FileBasedWAL;


@SpringBootTest(classes=BaseTestConfiguration.class)
public class FileBasedTransactionWALTest extends AbstractTransactionWALTest {

  @TestConfiguration
  @EnableTransactionManagement
  @EnableAutoConfiguration
  public static class ServiceConfig {
    @Bean
    WriteAheadLog fileBasedWal() {
      return new FileBasedWAL();
    }

    @Bean
    @RequestScope
    TransactionWAL wal() {
      return new DefaultTransactionWAL();
    }
  }

  @Autowired
  TransactionWAL wal;

  @Autowired
  TransactionTemplate transactionTemplate;
  
}
