package com.neverpile.eureka.objectstore.ehcache;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Ehcache object store.
 */
@Component
@ConfigurationProperties(prefix = "neverpile-eureka.storage.ehcache", ignoreUnknownFields = true)
public class EhcacheConfig {

  private boolean enabled;
  private String rootPath = "./neverpile-eureka_default";
  private String heapEntries = "500"; // around 5GB if we say one entry has 10MB
  private String diskSize = "20480";
  private boolean persistent = false;
  /**
   * The cache entry expiration time in minutes
   * @deprecated use {@link #expiryTime} instead.
   */
  @Deprecated(since = "1.11.0", forRemoval = true)
  private String expiryTimeMinutes;
  /**
   * The cache entry expiration time. If a duration suffix is not specified, minutes will be used.
   */
  @DurationUnit(ChronoUnit.MINUTES)
  private Duration expiryTime = Duration.ofMinutes(3);

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

  public boolean isPersistent() {
    return persistent;
  }

  public void setPersistent(boolean persistent) {
    this.persistent = persistent;
  }

  public String getExpiryTimeMinutes() {
    return Objects.requireNonNullElseGet(expiryTimeMinutes, () -> "" + expiryTime.toMinutes());
  }

  public void setExpiryTimeMinutes(String expiryTimeMinutes) {
    this.expiryTimeMinutes = expiryTimeMinutes;
  }

  public Duration getExpiryTime() {
    if (expiryTimeMinutes != null) {
      return Duration.ofMinutes(Long.parseLong(expiryTimeMinutes));
    }
    return expiryTime;
  }

  public void setExpiryTime(Duration expiryTime) {
    this.expiryTime = expiryTime;
  }
}