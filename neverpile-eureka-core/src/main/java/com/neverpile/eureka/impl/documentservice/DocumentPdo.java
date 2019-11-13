package com.neverpile.eureka.impl.documentservice;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.neverpile.eureka.model.Document;

/**
 * A class representing the way {@link DefaultDocumentService} and
 * {@link DefaultMultiVersioningDocumentService} persist documents along with their associated facet
 * data to the underlying store.
 */
public class DocumentPdo extends Document {

  private boolean deleted;

  private Map<String, JsonNode> associatedFacetData = new HashMap<>();

  public DocumentPdo() {
    // nothing to do
  }
  
  public DocumentPdo(final String id) {
    super(id);
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(final boolean deleted) {
    this.deleted = deleted;
  }
  
  public Map<String, JsonNode> getAssociatedFacetData() {
    return associatedFacetData;
  }

  public void setAssociatedFacetData(final Map<String, JsonNode> sidecar) {
    this.associatedFacetData = sidecar;
  }

  @JsonIgnore
  public JsonNode getSidecarElement(final String key) {
    return associatedFacetData.get(key);
  }

  @JsonIgnore
  public void putSidecarElement(final String key, final JsonNode node) {
    this.associatedFacetData.put(key, node);
  }

}
