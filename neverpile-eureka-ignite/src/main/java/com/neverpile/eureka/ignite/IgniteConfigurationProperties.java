package com.neverpile.eureka.ignite;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the neverpile ignite support.
 * 
 * Some of these properties are not used directly but just to document properties defined using
 * {@code @Value} and {@code @ConditionalOnProperty} constructs.
 */
@Component
@ConfigurationProperties(prefix = "neverpile-eureka.ignite", ignoreUnknownFields = true)
public class IgniteConfigurationProperties {
  public static class Persistence {
    private boolean enabled;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(final boolean enabled) {
      this.enabled = enabled;
    }
  }

  public enum DiscoveryMethod {
    NONE, MULTICAST, STATIC, FILESYSTEM
  }

  private boolean enabled;

  private DiscoveryMethod discovery = DiscoveryMethod.NONE;

  private Persistence persistence = new Persistence();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public Persistence getPersistence() {
    return persistence;
  }

  public void setPersistence(final Persistence persistence) {
    this.persistence = persistence;
  }

  public DiscoveryMethod getDiscovery() {
    return discovery;
  }

  public void setDiscovery(final DiscoveryMethod discovery) {
    this.discovery = discovery;
  }
}
