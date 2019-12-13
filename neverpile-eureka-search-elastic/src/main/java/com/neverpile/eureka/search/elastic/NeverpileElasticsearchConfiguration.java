package com.neverpile.eureka.search.elastic;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the neverpile elasticsearch subsystem.
 * 
 * Some of these properties are not used directly but just to document properties defined using
 * {@code @Value} and {@code @ConditionalOnProperty} constructs.
 */
@Component
@ConfigurationProperties("neverpile-eureka.elastic")
public class NeverpileElasticsearchConfiguration {
  /**
   * Whether to enable the elasticsearch subsystem.
   */
  private boolean enabled;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }
}
