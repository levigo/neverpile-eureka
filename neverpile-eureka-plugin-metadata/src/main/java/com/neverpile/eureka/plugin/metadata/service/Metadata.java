package com.neverpile.eureka.plugin.metadata.service;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.Versioned;

import java.util.HashMap;
import java.util.Map;

public class Metadata extends HashMap<String, MetadataElement> implements Map<String, MetadataElement>, Versioned{
  private static final long serialVersionUID = 1L;

  private String version = ObjectStoreService.NEW_VERSION;

  public String getVersion() {
    return version;
  }
  public void setVersion(String version) {
    this.version = version;
  }
}
