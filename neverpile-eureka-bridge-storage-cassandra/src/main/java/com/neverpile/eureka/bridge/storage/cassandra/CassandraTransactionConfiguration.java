package com.neverpile.eureka.bridge.storage.cassandra;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CassandraTransactionConfiguration implements ApplicationContextAware {

  public static ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext newApplicationContext) throws BeansException {
    applicationContext = newApplicationContext;
  }
}
