package com.neverpile.eureka.objectstore.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the S3 object store.
 */
@Component
@ConfigurationProperties(prefix = "neverpile-eureka.storage.s3", ignoreUnknownFields = true)
public class S3ObjectStoreConfiguration  {

  private boolean enabled;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }
}
