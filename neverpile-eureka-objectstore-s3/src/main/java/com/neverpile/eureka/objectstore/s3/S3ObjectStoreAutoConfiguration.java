package com.neverpile.eureka.objectstore.s3;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import com.neverpile.eureka.autoconfig.NeverpileEurekaAutoConfiguration;

@Configuration
@ConditionalOnProperty(name = "neverpile-eureka.storage.s3.enabled", havingValue = "true", matchIfMissing = false)
@Import({
    S3ConnectionConfiguration.class, S3ObjectStoreConfiguration.class
})
@AutoConfigureBefore(value = NeverpileEurekaAutoConfiguration.class)

// we want to provide our goods before NeverpileEurekaAutoConfiguration etc.
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class S3ObjectStoreAutoConfiguration {
  @Bean
  S3ObjectStoreService s3ObjectStoreService() {
    return new S3ObjectStoreService();
  }
}
