package com.neverpile.eureka.objectstore.ehcache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the EHCache object store.
 */
@Component
@ConfigurationProperties(prefix = "neverpile-eureka.storage.ehcache", ignoreUnknownFields = true)
public class EhcacheConfig {

  private boolean enabled;
  private String rootPath = "./neverpile-eureka_default";
  private String heapEntries = "500"; // around 5GB if we say one entry has 10MB
  private String diskSize = "20480";
  private String expiryTimeMinutes = "3";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public String getHeapEntries() {
    return heapEntries;
  }

  public void setHeapEntries(String heapEntries) {
    this.heapEntries = heapEntries;
  }

  public String getDiskSize() {
    return diskSize;
  }

  public void setDiskSize(String diskSize) {
    this.diskSize = diskSize;
  }

  public String getExpiryTimeMinutes() {
    return expiryTimeMinutes;
  }

  public void setExpiryTimeMinutes(String expiryTimeMinutes) {
    this.expiryTimeMinutes = expiryTimeMinutes;
  }
}