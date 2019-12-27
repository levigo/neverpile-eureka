package com.neverpile.eureka.plugin.audit.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;
import com.neverpile.eureka.plugin.audit.storage.AuditStorageBridge;
import com.neverpile.eureka.plugin.audit.verification.VerificationService;

public class DefaultAuditLogService implements AuditLogService {
  @Autowired
  private VerificationService verificationService;

  @Autowired
  private AuditStorageBridge auditLogStorageBridge;

  @Override
  public List<AuditEvent> getEventLog(final String documentId) {
    return auditLogStorageBridge.getDocumentAuditLog(documentId);
  }

  @Override
  public void logEvent(final AuditEvent event) {
    auditLogStorageBridge.putAuditEvent(event);
    verificationService.processEvent(event);
  }

  @Override
  public Optional<AuditEvent> getEvent(String auditId) {
    return auditLogStorageBridge.getAuditEvent(auditId);
  }
}