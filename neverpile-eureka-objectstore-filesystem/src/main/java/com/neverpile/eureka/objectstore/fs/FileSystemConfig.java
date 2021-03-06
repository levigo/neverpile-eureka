package com.neverpile.eureka.objectstore.fs;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.autoconfig.NeverpileEurekaAutoConfiguration;

@Configuration
@ConditionalOnMissingBean(ObjectStoreService.class)
@AutoConfigureBefore(
    value = NeverpileEurekaAutoConfiguration.class)
// we want to provide our goods before NeverpileEurekaAutoConfiguration etc but after other object
// stores.
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 1)
public class FileSystemConfig {

  @Bean
  ObjectStoreService fsObjectStoreService() {
    return new FilesystemObjectStoreService();
  }

}