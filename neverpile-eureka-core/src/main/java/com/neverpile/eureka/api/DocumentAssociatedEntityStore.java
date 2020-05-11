package com.neverpile.eureka.api;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.neverpile.eureka.model.Document;

/**
 * A document sidecar store provides persistence for document-related data. Data to be persisted is
 * transferred in the form of Jackson {@link JsonNode}s.
 */
public interface DocumentAssociatedEntityStore {
  /**
   * Attach data with an associated key to a {@link Document}. If there is already stored data using the same key,
   * the old data will be overwritten.
   *
   * @param document {@link Document} to associate the data with.
   * @param key      unique key for the associated data.
   * @param value    the data to be stored as {@link JsonNode}.
   */
  void store(Document document, String key, JsonNode value);

  /**
   * Retrieve previously stored data from a {@link Document} as {@link JsonNode}. If no data is found an empty
   * {@link Optional} will be returned.
   *
   * @param document {@link Document} to get the data from.
   * @param key      unique key for the associated data.
   * @return the stored data if present.
   */
  Optional<JsonNode> retrieve(Document document, String key);

  /**
   * Delete associated data from a {@link Document}.
   *
   * @param document {@link Document} to delete the data from.
   * @param key      unique key for the data to delete.
   */
  void delete(Document document, String key);
}
