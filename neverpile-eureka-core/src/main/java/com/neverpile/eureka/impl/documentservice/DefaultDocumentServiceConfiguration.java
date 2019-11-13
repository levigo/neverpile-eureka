package com.neverpile.eureka.impl.documentservice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "neverpile-eureka.document-service", ignoreUnknownFields = true)
public class DefaultDocumentServiceConfiguration {
  private String prefix[] = new String[]{
      "document"
  };
  
  private boolean enableMultiVersioning = true;

  public String[] getPrefix() {
    return prefix;
  }

  public void setPrefix(final String prefix[]) {
    this.prefix = prefix;
  }

  public boolean isEnableMultiVersioning() {
    return enableMultiVersioning;
  }

  public void setEnableMultiVersioning(final boolean enableMultiVersioning) {
    this.enableMultiVersioning = enableMultiVersioning;
  }
}
