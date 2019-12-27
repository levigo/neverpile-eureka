package com.neverpile.eureka.plugin.audit.verification;

import java.util.List;

import com.neverpile.eureka.plugin.audit.service.AuditEvent;

/**
 * Service to handle Cryptographic security layer for AuditLogs. This Service is responsible for creating the
 * appropriate data structure to ensure the consistency of audit log data.
 */
public interface HashStrategyService {

  /**
   * Add new event to secure its integrity in the audit log context for later verification.
   * Save verification for this events persistently.
   *
   * @param auditEvent Event to construct verification for.
   */
  public void addElement(AuditEvent auditEvent);

  /**
   * Add new events to secure their integrity in the audit log context for later verification.
   * Save verification for all passed events persistently.
   *
   * @param auditEvents Events to construct verification for.
   */
  public void addElements(List<AuditEvent> auditEvents);

  /**
   * Verifies the integrity of the given event in context of all verified audit logs.
   *
   * @param auditEvent Event to get verification for.
   * @return {@code true} if hash has successfully been verified - {@code false} otherwise.
   */
  public boolean verifyHash(AuditEvent auditEvent);

  /**
   * Verifies the integrity of all audit events.
   *
   * @return {@code true} if all Audit Events were successfully verified - {@code false} otherwise.
   */
  public boolean completeVerification();

}
