package com.neverpile.eureka.plugin.audit.service;

import java.util.List;
import java.util.Optional;

/**
 * An Audit Log service manages Incoming audits and provides an implementation to provide requested events.
 * Verification can be reqested for a single Event or the Complete Log including
 */
public interface AuditLogService {

  /**
   * Retrieve all AuditEvents associated with a document specified by its ID.
   *
   * @param documentId the Document ID for the document to collect all AuditEvents for.
   * @return
   */
  List<AuditEvent> getEventLog(String documentId);

  /**
   * Log a newly created AuditEvent.
   *
   * @param event the Audit Event to log.
   */
  void logEvent(AuditEvent event);

  /**
   * Retrieve a single AuditEvent by its ID.
   *
   * @param auditId ID if the AuditEvent to get.
   * @return the requested AuditEvent.
   */
  Optional<AuditEvent> getEvent(String auditId);
}
