package com.neverpile.eureka.impl.documentservice;

import java.util.UUID;

import com.neverpile.eureka.api.DocumentIdGenerationStrategy;

public class UuidDocumentIdGenerationStrategy implements DocumentIdGenerationStrategy {
  private static final String UUID_PATTERN = "\\p{XDigit}{8}-(\\p{XDigit}{4}-){3}\\p{XDigit}{12}";

  @Override
  public String createDocumentId() {
    return UUID.randomUUID().toString();
  }

  @Override
  public boolean validateDocumentId(final String id) {
    return id.matches(UUID_PATTERN);
  }
}
