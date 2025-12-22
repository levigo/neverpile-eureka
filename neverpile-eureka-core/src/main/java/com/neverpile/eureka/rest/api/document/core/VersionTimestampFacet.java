package com.neverpile.eureka.rest.api.document.core;

import java.time.Instant;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.DocumentFacet;

@Component
public class VersionTimestampFacet implements DocumentFacet<String> {
  @Override
  public String getName() {
    return "versionTimestamp";
  }

  @Override
  public JavaType getValueType(final TypeFactory f) {
    return f.constructType(Instant.class);
  }

  @Override
  public void onRetrieve(final Document document, final DocumentDto dto) {
    dto.setVersionTimestamp(document.getVersionTimestamp());
  }
  
  @Override
  public String toString() {
    return "DocumentFacet{" + "name=" + getName() + '}';
  }
}
