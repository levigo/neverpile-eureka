package com.neverpile.eureka.plugin.audit.service;

public interface AuditIdGenerationStrategy {
  String createAuditId(String documentId);

  boolean validateDocumentId(String id);

  String getDocumentId(String id);

  String getBlockId(String id);
}
