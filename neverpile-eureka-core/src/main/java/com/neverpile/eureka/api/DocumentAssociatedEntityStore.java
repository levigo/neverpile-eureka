package com.neverpile.eureka.api;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.neverpile.eureka.model.Document;

/**
 * A document sidecar store provides persistence for document-related data. Data to be persisted is
 * transferred in the form of Jackson {@link JsonNode}s.
 */
public interface DocumentAssociatedEntityStore {
  void store(Document document, String key, JsonNode value);

  Optional<JsonNode> retrieve(Document document, String key);
  
  void delete(Document document, String key);
}
