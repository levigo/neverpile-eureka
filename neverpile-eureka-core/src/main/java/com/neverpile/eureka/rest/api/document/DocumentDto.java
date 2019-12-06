package com.neverpile.eureka.rest.api.document;


import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Document", description = "A neverpile document")
@JsonPropertyOrder({
    "documentId", "versionTimestamp", "dateCreated", "dateModified", "contentElements"
})
public class DocumentDto extends ResourceSupport implements IDto {
  private String documentId;
  
  private Instant versionTimestamp;

  private final Map<String, Object> facets = new HashMap<>();

  public DocumentDto() {
  }

  @Schema(description = "The document's unique ID")
  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(final String documentId) {
    this.documentId = documentId;
  }

  public Instant getVersionTimestamp() {
    return versionTimestamp;
  }

  public void setVersionTimestamp(final Instant versionTimestamp) {
    this.versionTimestamp = versionTimestamp;
  }
  
  @Schema(hidden = true)
  @JsonAnyGetter
  public Map<String, Object> getFacets() {
    return facets;
  }

  @Schema(hidden = true)
  @JsonIgnore // handled by custom deserializer
  public void setFacet(final String name, final Object value) {
    this.facets.put(name, value);
  }
  
  @Schema(hidden = true)
  @JsonIgnore
  @SuppressWarnings("unchecked")
  public <V> Optional<V> getFacetData(final DocumentFacet<V> facet) {
    return (Optional<V>) Optional.ofNullable(facets.get(facet.getName()));
  }
  
  @Schema(hidden = true)
  @JsonIgnore
  public <V> void putFacetData(final DocumentFacet<V> facet, final V value) {
    facets.put(facet.getName(), value);
  }

}
