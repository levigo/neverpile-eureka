package com.neverpile.eureka.plugin.metadata.rest;

import static com.neverpile.eureka.plugin.metadata.rest.MetadataUtils.linkify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.jayway.jsonpath.JsonPath;
import com.mycila.xmltool.XMLDoc;
import com.neverpile.common.authorization.api.AuthorizationContext;
import com.neverpile.common.specifier.Specifier;
import com.neverpile.eureka.api.DocumentAuthorizationService;
import com.neverpile.eureka.api.index.Schema;
import com.neverpile.eureka.api.index.Structure;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.plugin.metadata.service.Metadata;
import com.neverpile.eureka.plugin.metadata.service.MetadataElement;
import com.neverpile.eureka.plugin.metadata.service.MetadataService;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.DocumentFacet;
import com.neverpile.eureka.rest.api.exception.NotFoundException;

/**
 * <h3>Authorization context</h3>
 * <p>
 * The following keys are supported:
 * </p>
 * <ul>
 * <li><code>""</code> (the empty string): existence of this facet (always true when facet
 * installed)
 * <li><code>&lt;name&gt;</code>: existence of the element with the requested name
 * <li><code>&lt;name&gt;.&lt;propertyName&gt;</code>: properties of the MetadataElement class
 * <li><code>&lt;name&gt;.json.&lt;jsonPath&gt;</code>: the given json path evaluated against
 * JSON-based metadata
 * <li><code>&lt;name&gt;.xml.&lt;XPath&gt;</code>: the given XPath path evaluated against XML-based
 * metadata
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "neverpile.facet.metadata.enabled", matchIfMissing = true)
@Import({
    MetadataMappingConfigurer.class
})
public class MetadataFacet implements DocumentFacet<MetadataDto> {
  static final String NAME = "metadata";

  @Autowired
  ModelMapper modelMapper;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  MetadataService metadataService;

  @Autowired
  DocumentAuthorizationService documentAuthorizationService;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public JavaType getValueType(final TypeFactory f) {
    return f.constructType(MetadataDto.class);
  }

  @Override
  public void onRetrieve(final Document document, final DocumentDto dto) {
    retrieve(document).ifPresent(mdDto -> dto.setFacet(getName(), mdDto));

    linkify(document, dto);
  }

  Optional<MetadataDto> retrieve(final Document document) {
    return metadataService.get(document).map(metadata -> toDto(document, metadata));
  }

  private MetadataDto toDto(final Document document, final Metadata metadata) {
    MetadataDto dto = modelMapper.map(metadata, MetadataDto.class);

    removeDisallowedEntries(document, dto);

    linkify(document, dto);
    return dto;
  }

  private void removeDisallowedEntries(final Document document, final MetadataDto dto) {
    for (Iterator<Entry<String, MetadataElementDto>> i = dto.getElements().entrySet().iterator(); i.hasNext();) {
      Map.Entry<String, MetadataElementDto> e = i.next();
      if (!documentAuthorizationService.authorizeSubresourceGet(document, getName(), e.getKey()))
        i.remove();
    }
  }

  @Override
  public void beforeCreate(final Document document, final DocumentDto requestDto) {
    MetadataDto metadata = (MetadataDto) requestDto.getFacets().get(getName());
    if (null != metadata) {
      metadata.get().forEach((name, element) -> {
        if (!documentAuthorizationService.authorizeSubresourceCreate(document, getName(), name))
          throw new AccessDeniedException("Creation of metadata element " + name + " denied");
      });

      metadataService.store(document, modelMapper.map(metadata, Metadata.class));
    }
  }

  @Override
  public void beforeUpdate(final Document current, final Document updated, final DocumentDto updateDto) {
    updateDto.getFacetData(this).ifPresent(metadata -> update(current, metadata));
  }

  MetadataDto update(final Document document, final MetadataDto metadata) {
    Metadata updated = modelMapper.map(metadata, Metadata.class);

    Metadata existing = metadataService.get(document).orElseGet(Metadata::new);

    // authorize element creation/update
    updated.forEach((name, element) -> authorizeCreateUpdate(document, existing, name, element));

    // authorize element deletion
    existing.keySet().stream() //
        .filter(k -> updated.get(k) == null) //
        .filter(k -> !documentAuthorizationService.authorizeSubresourceDelete(document, getName(), k)) //
        .findAny().ifPresent(k -> {
          throw new AccessDeniedException("Creation of metadata element " + k + " denied");
        });

    existing.clear();
    existing.putAll(updated);

    return toDto(document, metadataService.store(document, updated));
  }

  private void authorizeCreateUpdate(final Document document, final Metadata existing, final String name,
      final MetadataElement update) {
    MetadataElement existingElement = existing.get(name);
    if (null != existingElement) {
      if (!Objects.equals(update, existingElement)
          && !documentAuthorizationService.authorizeSubresourceUpdate(document, getName(), name))
        throw new AccessDeniedException("Update of metadata element " + name + " denied");
    } else {
      if (!documentAuthorizationService.authorizeSubresourceCreate(document, getName(), name))
        throw new AccessDeniedException("Creation of metadata element " + name + " denied");
    }
  }

  MetadataElementDto update(final Document document, final String name, final MetadataElementDto updateDto) {
    Metadata existing = metadataService.get(document).orElseGet(Metadata::new);

    MetadataElement update = modelMapper.map(updateDto, MetadataElement.class);

    authorizeCreateUpdate(document, existing, name, update);

    existing.put(name, update);

    Metadata stored = metadataService.store(document, existing);
    if (null == stored) // should not happen
      throw new NotFoundException("Stored element not found");

    return toDto(document, name, stored.get(name));
  }

  /**
   * Delete a single metadata element with the given name belonging to the given document.
   *
   * @param document the owning document
   * @param name the element name
   */
  void delete(final Document document, final String name) {
    if (!documentAuthorizationService.authorizeSubresourceDelete(document, getName(), name))
      throw new AccessDeniedException("Deletion of metadata element " + name + " denied");

    Metadata existing = metadataService.get(document).orElseGet(Metadata::new);

    if (existing.get(name) == null)
      throw new NotFoundException("Metadata element " + name + " does not exist");

    existing.remove(name);

    metadataService.store(document, existing);
  }

  private MetadataElementDto toDto(final Document document, final String name, final MetadataElement metadataElement) {
    MetadataElementDto metadataElementDto = modelMapper.map(metadataElement, MetadataElementDto.class);

    linkify(document, name, metadataElementDto);

    return metadataElementDto;
  }

  @Override
  public void onDelete(final Document document) {
    metadataService.delete(document);
  }

  @Override
  public AuthorizationContext getAuthorizationContextContribution(final Document document) {
    return new AuthorizationContext() {
      @Override
      public Object resolveValue(final Specifier elementKey) {
        if (elementKey.empty())
          return true; // signal just the fact that the metadata facet exists

        Metadata metadata = metadataService.get(document).orElseGet(Metadata::new);

        if (!elementKey.hasMore())
          return metadata.containsKey(elementKey.head()); // signal whether element exists

        MetadataElement metadataElement = metadata.get(elementKey.head());

        Specifier propertyKey = elementKey.suffix();
        if (metadataElement != null) {
          switch (propertyKey.head()){
            case "schema" :
              return metadataElement.getSchema();

            case "contentType" :
              return metadataElement.getContentType();

            case "content" :
              return metadataElement.getContent();

            case "dateCreated" :
              return metadataElement.getDateCreated();

            case "dateModified" :
              return metadataElement.getDateModified();

            case "encryption" :
              return metadataElement.getEncryption().name();

            case "json" :
              if (propertyKey.hasMore() && metadataElement.getContentType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                JsonPath jsonPath = JsonPath.compile(propertyKey.suffix().asString());
                return JsonPath.parse(new ByteArrayInputStream(metadataElement.getContent())).read(jsonPath);
              }
              break;

            case "xml" :
              if (propertyKey.hasMore() && (metadataElement.getContentType().equals(MediaType.APPLICATION_XML_TYPE)
                  || metadataElement.getContentType().equals(MediaType.TEXT_XML_TYPE))) {
                String xPath = propertyKey.suffix().asString();
                return XMLDoc.from(new ByteArrayInputStream(metadataElement.getContent()), true) //
                    .rawXpathString(xPath);
              }
              break;
          }
        }

        return null;
      }
    };
  }

  @Override
  public Schema getIndexSchema() {
    return new Structure().withDynamicFields();
  }

  @Override
  public JsonNode getIndexData(final Document document) {
    return metadataService.get(document) //
        .map(this::extractMetadata) //
        .orElseGet(() -> objectMapper.createObjectNode());
  }

  private JsonNode extractMetadata(final Metadata metadata) {
    ObjectNode metadataNode = objectMapper.createObjectNode();
    
    metadata.forEach((name, element) -> metadataNode.set(name, extractMetadataElement(element)));
    
    return metadataNode;
  }
  
  private JsonNode extractMetadataElement(final MetadataElement element) {
    // FIXME: consider using Spring's MediaType
    switch(element.getContentType().getType() + "/" + element.getContentType().getSubtype()) {
      case MediaType.APPLICATION_JSON:
        return extractJsonElement(element);
        
      case MediaType.APPLICATION_XML:
        return extractXmlElement(element);
        
      default:
        return objectMapper.createObjectNode();
    }
  }

  private JsonNode extractXmlElement(final MetadataElement element) {
    try {
      return new XmlMapper().readTree(element.getContent());
    } catch (Exception e) {
      return objectMapper.createObjectNode().put("%%%NEPVERPILE_EUREKA_METADATA_ERROR%%%", "Can't extract XML metadata: " + e.getMessage());
    }
  }

  private JsonNode extractJsonElement(final MetadataElement element) {
    try {
      return objectMapper.readTree(element.getContent());
    } catch (IOException e) {
      return objectMapper.createObjectNode().put("%%%NEPVERPILE_EUREKA_METADATA_ERROR%%%", "Can't extract JSON metadata: " + e.getMessage());
    }
  }

  @Override
  public String toString() {
    return "DocumentFacet{" + "name=" + getName() + '}';
  }
}
