package com.neverpile.eureka.objectstore.oam;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.neverpile.eureka.api.ObjectStoreService;

@Configuration
@ConditionalOnProperty(
    name = "neverpile-eureka.oam.enabled",
    havingValue = "true",
    matchIfMissing = true)
@ComponentScan
public class OamAutoConfiguration {

  @Bean
  ObjectStoreService fsObjectStoreService() {
    return new OamObjectStoreService();
  }

}
