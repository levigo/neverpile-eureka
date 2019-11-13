package com.neverpile.eureka.hazelcast;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

import com.hazelcast.config.Config;

/**
 * Configuration properties for the neverpile hazelcast support.
 * 
 * Some of these properties are not used directly but just to document properties defined using
 * {@code @Value} and {@code @ConditionalOnProperty} constructs.
 */
@Component
@ConfigurationProperties(prefix = "neverpile-eureka.hazelcast", ignoreUnknownFields = true)
public class HazelcastConfigurationProperties {
  private boolean enabled;
  
  private int minimumClusterSize = 2;
  
  @NestedConfigurationProperty
  private Config configuration = new Config();

  public int getMinimumClusterSize() {
    return minimumClusterSize;
  }

  public void setMinimumClusterSize(final int minimumClusterSize) {
    this.minimumClusterSize = minimumClusterSize;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public Config getConfiguration() {
    return configuration;
  }

  public void setConfiguration(final Config config) {
    this.configuration = config;
  }
}
