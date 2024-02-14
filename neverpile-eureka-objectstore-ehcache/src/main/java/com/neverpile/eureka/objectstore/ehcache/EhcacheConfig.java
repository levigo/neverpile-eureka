package com.neverpile.eureka.objectstore.ehcache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.autoconfig.NeverpileEurekaAutoConfiguration;

@Configuration
@ConditionalOnProperty(name = "neverpile-eureka.storage.ehcache.enabled", havingValue = "true", matchIfMissing = false)
@AutoConfigureBefore(
    value = NeverpileEurekaAutoConfiguration.class)
// we want to provide our goods before NeverpileEurekaAutoConfiguration etc
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class EhcacheConfig {

  @Value("${neverpile-eureka.storage.ehcache.rootPath:./neverpile-eureka_default}")
  private String rootPath = "./neverpile-eureka_default";
  @Value("${neverpile-eureka.storage.ehcache.heapEntries:500}")
  private String heapEntries = "500"; // around 5GB if we say one entry has 10MB
  @Value("${neverpile-eureka.storage.ehcache.diskSize:20480}")
  private String diskSize = "20480";
  @Value("${neverpile-eureka.storage.ehcache.expiryTimeMinutes:3}")
  private String expiryTimeMinutes = "3";

  @Bean
  ObjectStoreService ehcacheObjectStoreService() {
    return new EhcacheObjectStoreService(rootPath, heapEntries, diskSize, expiryTimeMinutes);
  }

}