package com.neverpile.eureka.ignite;

import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.neverpile.eureka.ignite.IgniteConfigurationProperties.Finders.S3.MutableAWSCredentials;

@Configuration
@ConditionalOnProperty(name = "neverpile-eureka.ignite.enabled", havingValue = "true", matchIfMissing = false)
public class NeverpileIgniteAWSAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean(TcpDiscoveryIpFinder.class)
  @ConfigurationProperties("neverpile-eureka.ignite.finder.s3")
  @ConditionalOnProperty("neverpile-eureka.ignite.finder.s3.enabled")
  public TcpDiscoveryIpFinder cloudIpFinder(final IgniteConfigurationProperties properites) {
    System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");

    org.apache.ignite.spi.discovery.tcp.ipfinder.s3.TcpDiscoveryS3IpFinder finder = new org.apache.ignite.spi.discovery.tcp.ipfinder.s3.TcpDiscoveryS3IpFinder();
    MutableAWSCredentials c = properites.getFinder().getS3().getAwsCredentials();
    finder.setAwsCredentials(new BasicAWSCredentials(c.getAccessKey(), c.getSecretKey()));
    
    return finder;
  }
}
