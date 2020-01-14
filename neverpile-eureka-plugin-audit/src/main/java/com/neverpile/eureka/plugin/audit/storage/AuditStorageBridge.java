package com.neverpile.eureka.plugin.audit.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.verification.VerificationService;

/**
 * This interface allows the audit plugin to use a variety of storage mediums depending on the implementation.
 * This interface is used for storing the AuditEvents itself and and the verification used by the {@link VerificationService}
 */
public interface AuditStorageBridge {

  /**
   * The AuditLog Event will be persisted here and can be retrieved later.
   *
   * @param auditEvent the audit event to store.
   */
  public void putAuditEvent(AuditEvent auditEvent);

  /**
   * Retrieved a persisted AuditLog Event by its ID.
   *
   * @param auditId the audit event ID to retrieve the audit event for.
   * @return the requested AuditEvent.
   */
  public Optional<AuditEvent> getAuditEvent(String auditId);

  /**
   * Get all AuditEvents registered for the given Document ID as a list.
   *
   * @param documentId the document ID to list all audit events for.
   * @return the requested AuditEvent List.
   */
  public List<AuditEvent> getDocumentAuditLog(String documentId);

  /**
   * Store a implementation dependent Verification Object with the given ObjectName as a key for later verification.
   *
   * @param key A unique objectName as a key for the given audit verification data.
   * @param verificationElement InputStream of any verification data.
   * @param length length of the given InputStream.
   */
  public void putVerificationElement(ObjectName key, InputStream verificationElement, int length);

  /**
   * Retrieve a stored Verification Object by its ObjectName as a Key.
   *
   * @param key A unique objectName as a key for the requested audit verification data.
   * @return the requested Verification data as an InputStream.
   */
  public Optional<InputStream> getVerificationElement(ObjectName key);

  /**
   * Update the Current Verification Head of the underlying verification structure.
   * This head value is used by various verification data structures to append new data.
   *
   * @return the head Verification data as an InputStream.
   */
  public Optional<InputStream> getHeadVerificationElement();


  /**
   * Update the Current Verification Head of the underlying verification structure.
   * This head value has to be updated depending on the implementation to ensure new data can be appended consistently.
   *
   * @param verificationElement a new InputStream representing the new verification data head.
   * @param length length of the given InputStream.
   */
  public void updateHeadVerificationElement(InputStream verificationElement, int length);


}
