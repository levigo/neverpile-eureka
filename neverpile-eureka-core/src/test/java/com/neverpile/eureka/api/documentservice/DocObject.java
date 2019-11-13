package com.neverpile.eureka.api.documentservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.ObjectName;

final class DocObject implements ObjectStoreService.StoreObject {
  private final Document doc;
  private final ObjectName name;
  private final ObjectMapper mapper;

  DocObject(final ObjectMapper mapper, final Document doc, final ObjectName name) {
    this.mapper = mapper;
    this.doc = doc;
    this.name = name;
  }

  @Override
  public ObjectName getObjectName() {
    return name;
  }

  @Override
  public InputStream getInputStream() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      mapper.writeValue(baos, doc);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new ByteArrayInputStream(baos.toByteArray());
  }

  @Override
  public String getVersion() {
    return "0";
  }
}