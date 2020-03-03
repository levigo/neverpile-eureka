package com.neverpile.eureka.plugin.metadata.rest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.neverpile.eureka.rest.api.document.IDto;

public class MetadataDto extends RepresentationModel<MetadataDto> implements IDto {
  public static MetadataDto with(final String name, final MetadataElementDto metadata) {
    return new MetadataDto().set(name, metadata);
  }
  
  private final Map<String, MetadataElementDto> elements = new HashMap<String, MetadataElementDto>();
  
  @JsonAnyGetter
  public Map<String, MetadataElementDto> get() {
    return elements;
  }
  
  @JsonAnySetter
  public MetadataDto set(final String name, final MetadataElementDto element) {
    elements.put(name, element);
    return this;
  }

  @JsonIgnore
  public Map<String, MetadataElementDto> getElements() {
    return elements;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((elements == null) ? 0 : elements.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MetadataDto other = (MetadataDto) obj;
    if (elements == null) {
      if (other.elements != null)
        return false;
    } else if (!elements.equals(other.elements))
      return false;
    return true;
  }
}
