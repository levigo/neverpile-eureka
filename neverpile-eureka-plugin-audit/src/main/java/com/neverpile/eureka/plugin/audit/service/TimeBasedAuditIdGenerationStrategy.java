package com.neverpile.eureka.plugin.audit.service;

import java.util.Date;

public interface TimeBasedAuditIdGenerationStrategy extends AuditIdGenerationStrategy {
  public String createAuditId(Date timestamp, String documentId);
}
