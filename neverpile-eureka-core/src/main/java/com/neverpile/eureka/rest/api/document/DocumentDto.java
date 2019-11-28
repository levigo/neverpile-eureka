package com.neverpile.eureka.rest.api.document;


import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import springfox.documentation.annotations.ApiIgnore;

@ApiModel(value = "Document", description = "A neverpile document")
@JsonPropertyOrder({
    "documentId", "versionTimestamp", "dateCreated", "dateModified", "contentElements"
})
public class DocumentDto extends RepresentationModel<DocumentDto> implements IDto {
  private String documentId;
  
  private Instant versionTimestamp;

  private final Map<String, Object> facets = new HashMap<>();

  public DocumentDto() {
  }

  @ApiModelProperty("The document's unique ID")
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
  
  @ApiIgnore
  @JsonAnyGetter
  public Map<String, Object> getFacets() {
    return facets;
  }

  @ApiIgnore
  @JsonIgnore // handled by custom deserializer
  public void setFacet(final String name, final Object value) {
    this.facets.put(name, value);
  }
  
  @ApiIgnore
  @JsonIgnore
  @SuppressWarnings("unchecked")
  public <V> Optional<V> getFacetData(final DocumentFacet<V> facet) {
    return (Optional<V>) Optional.ofNullable(facets.get(facet.getName()));
  }
  
  @ApiIgnore
  @JsonIgnore
  public <V> void putFacetData(final DocumentFacet<V> facet, final V value) {
    facets.put(facet.getName(), value);
  }

}
