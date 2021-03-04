package com.neverpile.objectstore.oam;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for the OAM object store.
 */
@Component
@ConfigurationProperties(
    prefix = "neverpile-eureka.storage.oam",
    ignoreUnknownFields = true)
@Getter
@Setter
public class OamObjectStoreConfiguration {

  private boolean enabled;

}
