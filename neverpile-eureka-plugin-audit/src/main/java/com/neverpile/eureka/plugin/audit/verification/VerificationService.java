package com.neverpile.eureka.plugin.audit.verification;

import com.neverpile.eureka.plugin.audit.service.AuditEvent;

/**
 * Service to provide a Implementation to verify incoming audit Events and provide a way to verify their
 * integrity later.
 */
public interface VerificationService {
  /**
   * This function will be called for newly created audit events and provides the Service with information
   * to verify them later.
   *
   * @param auditEvent incoming AuditEvent.
   */
  void processEvent(AuditEvent auditEvent);

  /**
   * Provide a way to verify every known audit event and proof its validity and consistency.
   *
   * @param auditEvent AuditEvent to verify.
   * @return {@code true} if the Event is valid - {@code false} otherwise.
   */
  boolean verifyEvent(AuditEvent auditEvent);

  /**
   * Complete verification far all Audit events. Every audit event gets validated and if any event is invalid this
   * function will report the incident.
   *
   * @return {@code true} if every Event is valid - {@code false} otherwise.
   */
  boolean completeVerification();
}
