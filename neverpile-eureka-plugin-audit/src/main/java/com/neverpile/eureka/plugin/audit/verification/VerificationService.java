package com.neverpile.eureka.plugin.audit.verification;

import com.neverpile.eureka.plugin.audit.service.AuditEvent;

public interface VerificationService {
  void processEvent(AuditEvent auditEvent);

  boolean verifyEvent(AuditEvent auditEvent);

  boolean completeVerification();
}
