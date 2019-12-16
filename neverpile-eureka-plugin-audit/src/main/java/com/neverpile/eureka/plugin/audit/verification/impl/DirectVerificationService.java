package com.neverpile.eureka.plugin.audit.verification.impl;

import org.springframework.beans.factory.annotation.Autowired;

import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.verification.HashStrategyService;
import com.neverpile.eureka.plugin.audit.verification.VerificationService;

public class DirectVerificationService implements VerificationService {

  @Autowired
  HashStrategyService auditStructure;

  @Override
  public void processEvent(AuditEvent auditEvent) {
    auditStructure.addElement(auditEvent);
  }

  @Override
  public boolean verifyEvent(AuditEvent auditEvent) {
    return auditStructure.verifyHash(auditEvent);
  }

  @Override
  public boolean completeVerification() {
    return auditStructure.completeVerification();
  }
}
