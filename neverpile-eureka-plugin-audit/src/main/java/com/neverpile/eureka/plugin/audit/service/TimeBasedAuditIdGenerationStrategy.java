package com.neverpile.eureka.plugin.audit.service;

import java.time.Instant;

public interface TimeBasedAuditIdGenerationStrategy extends AuditIdGenerationStrategy {
  public String createAuditId(Instant timestamp, String documentId);
}
