package com.neverpile.objectstore.oam;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.neverpile.eureka.autoconfig.NeverpileEurekaAutoConfiguration;

@Configuration
@ConditionalOnProperty(
    name = "neverpile-eureka.storage.oam.enabled",
    havingValue = "true",
    matchIfMissing = false)
// @Import({
// S3ConnectionConfiguration.class, S3ObjectStoreConfiguration.class
// })
@AutoConfigureBefore(
    value = NeverpileEurekaAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class OamObjectStoreAutoConfiguration {

  @Bean
  OamObjectStoreService oamObjectStoreService() {
    return new OamObjectStoreService();
  }

}
