package com.neverpile.eureka.plugin.metadata.service.impl;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.api.DocumentAssociatedEntityStore;
import com.neverpile.eureka.impl.documentservice.DefaultDocumentService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.plugin.metadata.service.Metadata;
import com.neverpile.eureka.plugin.metadata.service.MetadataElement;
import com.neverpile.eureka.plugin.metadata.service.MetadataService;

public class SimpleMetadataService implements MetadataService {
  private static final String ASSOCIATED_ENTITY_KEY = "metadata";

  @Autowired
  private DocumentAssociatedEntityStore associatedEntityStore;

  @Autowired
  private ApplicationContext appContext;

  // See note on getObjectMapper().
  // @Autowired
  // private ObjectMapper objectMapper;

  @Override
  public Optional<Metadata> get(final Document document) {
    Optional<JsonNode> storedDocumentMetadata = associatedEntityStore.retrieve(document,
        ASSOCIATED_ENTITY_KEY);

    if (!storedDocumentMetadata.isPresent())
      return Optional.empty();

    return Optional.ofNullable(getObjectMapper().convertValue(storedDocumentMetadata.get(),
        getObjectMapper().getTypeFactory().constructType(Metadata.class)));
  }

  /**
   * This is a workaround for a very weird Spring problem: if we autowire an ObjectMapper in the
   * same way the {@link DefaultDocumentService} does, this changes the application context
   * initialization in some way so that the Jackson configuration in the REST module is no longer
   * applied, breaking tons of stuff.
   * 
   * FIXME: try to lose this workaround once we switched to Spring Boot 2
   * 
   * @return
   */
  private ObjectMapper getObjectMapper() {
    return appContext.getBean(ObjectMapper.class);
  }

  @Override
  public Metadata store(final Document document, final Metadata metadata) {
    Objects.requireNonNull(metadata, ASSOCIATED_ENTITY_KEY);

    // merge and/or update the lifecycle properties:
    Metadata existing = get(document).orElse(new Metadata());
    existing.forEach((name, element) -> {
      MetadataElement newElement = metadata.get(name);
      if (null != newElement)
        newElement.setDateCreated(element.getDateCreated());
    });

    Date now = new Date();
    metadata.forEach((name, element) -> {
      element.setDateModified(now);
      if (null == element.getDateCreated())
        element.setDateCreated(now);
    });

    // store into object store
    associatedEntityStore.store(document, ASSOCIATED_ENTITY_KEY,
        getObjectMapper().valueToTree(metadata));

    return metadata;
  }

  @Override
  public void delete(final Document document) {
    associatedEntityStore.delete(document, ASSOCIATED_ENTITY_KEY);
  }
}
