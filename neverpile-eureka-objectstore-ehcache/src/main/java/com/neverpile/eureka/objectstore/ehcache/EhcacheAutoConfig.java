package com.neverpile.eureka.objectstore.ehcache;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import com.neverpile.eureka.autoconfig.NeverpileEurekaAutoConfiguration;
import com.neverpile.eureka.objectstore.ehcache.simple.EhcacheSimpleObjectStoreService;

@Configuration
@ConditionalOnProperty(name = "neverpile-eureka.storage.ehcache.enabled", havingValue = "true", matchIfMissing = false)
@Import(EhcacheConfig.class)
@AutoConfigureBefore(value = NeverpileEurekaAutoConfiguration.class)

// we want to provide our goods before NeverpileEurekaAutoConfiguration etc
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class EhcacheAutoConfig {

  @Bean
  @ConditionalOnProperty(value = "neverpile-eureka.document-service.enable-multi-versioning", havingValue = "true")
  EhcacheObjectStoreService ehcacheObjectStoreService() {
    return new EhcacheObjectStoreService();
  }

  @Bean
  @ConditionalOnProperty(value = "neverpile-eureka.document-service.enable-multi-versioning", havingValue = "false")
  EhcacheSimpleObjectStoreService ehcacheSimpleObjectStoreService(EhcacheConfig ehcacheConfig) {
    return new EhcacheSimpleObjectStoreService(ehcacheConfig);
  }

}