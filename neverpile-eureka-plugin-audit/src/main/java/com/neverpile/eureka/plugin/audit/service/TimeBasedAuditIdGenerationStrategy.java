package com.neverpile.eureka.plugin.audit.service;

import java.time.Instant;

/**
 * This interfaces defines functions to create and validate such Audit IDs. The Id will be generated using using the
 * Document ID in combination with a given hig precision timestamp. the timestamp will bes used to distinguish audit
 * events within a single document and to generate the corresponding Block ID.
 */
public interface TimeBasedAuditIdGenerationStrategy extends AuditIdGenerationStrategy {

  /**
   * Creates a new AuditId for a given Document. This id is unique among all other Audit IDs.
   *
   * @param timestamp the creation Time of the AuditEvent.
   * @param documentId the Document ID to generate an new Audit ID for.
   * @return the newly generated Audit ID.
   */
  public String createAuditId(Instant timestamp, String documentId);
}
