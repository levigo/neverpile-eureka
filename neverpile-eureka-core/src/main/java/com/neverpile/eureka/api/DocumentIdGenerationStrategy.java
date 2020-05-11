package com.neverpile.eureka.api;

import com.neverpile.eureka.model.Document;

/**
 * {@link Document}s must have unique IDs. This interfaces defines functions to create and validate such IDs
 * with a implementation of choice.
 */
public interface DocumentIdGenerationStrategy {

  /**
   * Create a new {@link Document#documentId} for a {@link Document}.
   *
   * @return String representation of the newly generated ID.
   */
  String createDocumentId();

  /**
   * Validates an existing {@link Document#documentId}.
   *
   * @param id The {@link Document#documentId} String to be validated.
   * @return {@code true} if the ID is valid. {@code false} otherwise.
   */
  boolean validateDocumentId(String id);

}
