package com.neverpile.eureka.ignite;

import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "neverpile-eureka.ignite.enabled", havingValue = "true", matchIfMissing = false)
public class NeverpileIgniteCloudAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean(TcpDiscoveryIpFinder.class)
  @ConfigurationProperties("neverpile-eureka.ignite.finder.cloud")
  @ConditionalOnProperty("neverpile-eureka.ignite.finder.cloud.enabled")
  public TcpDiscoveryIpFinder cloudIpFinder() {
    return new org.apache.ignite.spi.discovery.tcp.ipfinder.cloud.TcpDiscoveryCloudIpFinder();
  }
}
