package com.neverpile.eureka.plugin.audit.service;

/**
 * This interfaces defines functions to create and validate such Audit IDs with a implementation of choice.
 */
public interface AuditIdGenerationStrategy {

  /**
   * Creates a new AuditId for a given Document. This id is unique among all other Audit IDs.
   *
   * @param documentId the Document ID to generate an new Audit ID for.
   * @return the newly generated Audit ID.
   */
  String createAuditId(String documentId);

  /**
   * Validates a given AuditId.
   *
   * @param id the Audit ID to validate.
   * @return {@code true} if the Id is valid - {@code false} otherwise.
   */
  boolean validateAuditId(String id);

  /**
   * Takes the ID of an AuditEvent and find the Document ID this event belongs to.
   *
   * @param id the AuditEvent ID to find the Document ID for.
   * @return the Document ID.
   */
  String getDocumentId(String id);

  /**
   * Takes the ID of an AuditEvent and find the Block ID this event belongs to.
   *
   * @param id the AuditEvent ID to find the Block ID for.
   * @return the Block ID.
   */
  String getBlockId(String id);
}
