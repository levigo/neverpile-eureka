package com.neverpile.eureka.impl.documentservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.common.opentelemetry.TraceInvocation;
import com.neverpile.eureka.api.DocumentAssociatedEntityStore;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.ObjectStoreService.ObjectStoreException;
import com.neverpile.eureka.api.ObjectStoreService.StoreObject;
import com.neverpile.eureka.api.exception.VersionMismatchException;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.rest.api.exception.NotFoundException;

/**
 * An implementation of {@link DocumentService} which stores all document metadata within an object
 * store. It does not implement multi-versioning and thus updates are destructive.
 * <p>
 * Basic optimistic concurrency control is implemented based on object store object version
 * tracking.
 */
public class DefaultDocumentService implements DocumentService, DocumentAssociatedEntityStore {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDocumentService.class);

  @Autowired
  EventPublisher eventPublisher;

  @Autowired
  private ObjectStoreService objectStore;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ModelMapper modelMapper;

  private static final String DOCUMENTPREFIX = "document";

  /**
   * A wrapper for a {@link Document} being handled within a transaction.
   */
  class TransactionalDocument {
    Optional<DocumentPdo> document;

    String version;

    final Map<String, JsonNode> associatedEntities;

    public TransactionalDocument(final Optional<DocumentPdo> document, final String version) {
      this.document = document;
      this.version = version;
      this.associatedEntities = document.map(d -> d.getAssociatedFacetData()).orElse(new HashMap<>());
    }

    public Document persist() {
      DocumentPdo doc = this.document.get();
      doc.setAssociatedFacetData(associatedEntities);
      return doPersistDocument(doc, version);
    }
  }

  class EntityRegistry {
    private final boolean mutable;

    final Map<String, TransactionalDocument> documents = new HashMap<>();

    final Set<String> modified = new HashSet<>();

    final Set<String> created = new HashSet<>();

    final Set<String> deleted = new HashSet<>();

    public EntityRegistry(final boolean mutable) {
      this.mutable = mutable;
    }

    private Map<String, JsonNode> associatedEntityMap(final String documentId) {
      return document(documentId).associatedEntities;
    }

    public TransactionalDocument document(final String documentId) {
      return documents.computeIfAbsent(documentId, DefaultDocumentService.this::doRetrieveDocument);
    }

    public void create(final DocumentPdo document) {
      if (!mutable)
        throw new IllegalStateException("Mutations not supported outside transactions");

      String id = document.getDocumentId();
      created.add(id);
      if (documents.containsKey(id))
        documents.get(id).document = Optional.of(document);
      else
        documents.put(id, new TransactionalDocument(Optional.of(document), ObjectStoreService.NEW_VERSION));
    }

    public void delete(final String documentId) {
      if (!mutable)
        throw new IllegalStateException("Mutations not supported outside transactions");

      deleted.add(documentId);
    }

    public Optional<DocumentPdo> update(final DocumentPdo updated) {
      if (!mutable)
        throw new IllegalStateException("Mutations not supported outside transactions");

      String id = updated.getDocumentId();

      TransactionalDocument current = document(id);
      Document currentDocument = current.document.orElseThrow(
          () -> new NotFoundException("Document " + id + " to be updated not found"));

      /*
       * Check version timestamps. We accept the update if either the updated timestamp is correct
       * or null, in which case we treat the update as non-version checking.
       */
      if (null != updated.getVersionTimestamp()
          && !currentDocument.getVersionTimestamp().equals(updated.getVersionTimestamp()))
        throw new VersionMismatchException("Document version timestamps do not match",
            currentDocument.getVersionTimestamp().toString(), updated.getVersionTimestamp().toString());

      // ok, we can go ahead with the update
      modified.add(id);
      current.document = Optional.of(updated);

      return current.document;
    }

    public void flush() {
      // flush creates
      created.stream().map(this::document).forEach(doc -> eventPublisher.publishCreateEvent(doc.persist()));

      // flush updates
      modified.removeAll(created); // already handled
      modified.stream().map(this::document).forEach(doc -> eventPublisher.publishUpdateEvent(doc.persist()));

      // flush deletes
      deleted.forEach(documentId -> {
        doDeleteDocument(documentId);
        eventPublisher.publishDeleteEvent(documentId);
      });
    }

    public void putAssociatedEntity(final String documentId, final String key, final JsonNode value) {
      associatedEntityMap(documentId).put(key, value);
      modified.add(documentId);
    }

    public void removeAssociatedEntity(final String documentId, final String key) {
      associatedEntityMap(documentId).remove(key);
      modified.add(documentId);
    }
  }

  @Order(Ordered.HIGHEST_PRECEDENCE) // Flush entities as soon as possible during commit phase
  private class FlushEntitiesSynchronization extends TransactionSynchronizationAdapter {
    private final EntityRegistry registry;

    public FlushEntitiesSynchronization(final EntityRegistry scopedObjects) {
      this.registry = scopedObjects;
    }

    @Override
    public void suspend() {
      TransactionSynchronizationManager.unbindResource(DefaultDocumentService.this);
    }

    @Override
    public void resume() {
      TransactionSynchronizationManager.bindResource(DefaultDocumentService.this, this.registry);
    }

    @Override
    public void beforeCommit(final boolean readOnly) {
      registry.flush();
    }

    @Override
    public void afterCompletion(final int status) {
      TransactionSynchronizationManager.unbindResourceIfPossible(DefaultDocumentService.this);
    }
  }

  EntityRegistry txEntityRegistry() {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      // return a read-only entity registry
      return new EntityRegistry(false);
    } else {
      EntityRegistry entityRegistry = (EntityRegistry) TransactionSynchronizationManager.getResource(this);
      if (entityRegistry == null) {
        entityRegistry = new EntityRegistry(true);
        TransactionSynchronizationManager.registerSynchronization(new FlushEntitiesSynchronization(entityRegistry));
        TransactionSynchronizationManager.bindResource(this, entityRegistry);
      }

      return entityRegistry;
    }
  }

  @Override
  @TraceInvocation
  public Optional<Document> getDocument(final String documentId) {
    return txEntityRegistry().document(documentId).document.map(d -> (Document) d);
  }

  @Override
  @TraceInvocation
  public Document createDocument(final Document document) {
    if (null != objectStore.get(createDocumentObjectName(document.getDocumentId())))
      throw new DocumentAlreadyExistsException(document);

    if (txEntityRegistry().document(document.getDocumentId()).document.isPresent())
      throw new DocumentAlreadyExistsException(document);

    ensureBasicMetadataPresent(document);

    txEntityRegistry().create(modelMapper.map(document, DocumentPdo.class));

    return document;
  }

  static void ensureBasicMetadataPresent(Document document) {
    // make sure that some basic metadata is present
    var now = Instant.now();
    if(document.getDateCreated() == null) {
      document.setDateCreated(now);
    }
    if(document.getDateModified() == null) {
      document.setDateModified(now);
    }
  }

  @Override
  @TraceInvocation
  public boolean deleteDocument(final String documentId) {
    txEntityRegistry().delete(documentId);
    return true;
  }

  @Override
  public Optional<Document> update(final Document deltaDocument) {
    ensureBasicMetadataPresent(deltaDocument);

    return txEntityRegistry().update(modelMapper.map(deltaDocument, DocumentPdo.class)).map(d -> (Document) d);
  }

  @Override
  @TraceInvocation
  public boolean documentExists(final String documentId) {
    ObjectName metadataObjectName = createDocumentObjectName(documentId);
    return objectStore.checkObjectExists(metadataObjectName);
  }

  @Override
  @TraceInvocation
  public Stream<String> getAllDocumentIds() {
    // @formatter:off
    // Second part of ObjectName is documentId. See 'createDocumentDirectoryName()'.
    return objectStore
        .list(ObjectName.of(DOCUMENTPREFIX))
        .filter(s -> s.getObjectName().to().length > 1)
        .map(s -> s.getObjectName().to()[1]);
    // @formatter:on

  }

  @Override
  @TraceInvocation
  public List<Document> getDocuments(final List<String> documentIds) {
    // @formatter:off
    return documentIds.stream()
        .map(this::getDocument)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
    // @formatter:on

  }

  @Override
  @TraceInvocation
  public void store(final Document document, final String key, final JsonNode value) {
    txEntityRegistry().putAssociatedEntity(document.getDocumentId(), key, value);
  }

  @Override
  @TraceInvocation
  public Optional<JsonNode> retrieve(final Document document, final String key) {
    return Optional.ofNullable(txEntityRegistry().associatedEntityMap(document.getDocumentId()).get(key));
  }

  @Override
  @TraceInvocation
  public void delete(final Document document, final String key) {
    txEntityRegistry().removeAssociatedEntity(document.getDocumentId(), key);
  }

  /**
   * Perform the actual retrieval of a document based on a given id.
   *
   * @param documentId the id of the document to retrieve
   * @return the document
   */
  private TransactionalDocument doRetrieveDocument(final String documentId) {
    ObjectName objectName = createDocumentObjectName(documentId);
    StoreObject storedDocument = objectStore.get(objectName);

    if (null == storedDocument)
      return new TransactionalDocument(Optional.empty(), ObjectStoreService.NEW_VERSION);

    try {
      DocumentPdo doc = objectMapper.readValue(storedDocument.getInputStream(), DocumentPdo.class);
      return new TransactionalDocument(Optional.of(doc), storedDocument.getVersion());
    } catch (IOException e) {
      LOGGER.error("Failed to deserialize document @{}", objectName, e);
      throw new DocumentServiceException("Failed to retrieve document");
    }
  }

  /**
   * Persist changes to or create the given document.
   *
   * @param document the document to persist
   * @return the document
   */
  private Document doPersistDocument(final DocumentPdo document, final String version) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
      objectMapper.writeValue(baos, document);

      ObjectName objectName = createDocumentObjectName(document.getDocumentId());
      try {
        objectStore.put(objectName, version, new ByteArrayInputStream(baos.toByteArray()), baos.size());
        return document;
      } catch (ObjectStoreException e) {
        LOGGER.error("Failed to store document @{}", objectName, e);
        throw new DocumentServiceException("Failed to store document");
      }
    } catch (IOException e) {
      LOGGER.error("Failed to serialize document", e);
      throw new DocumentServiceException("Failed to serialize document");
    }
  }

  protected ObjectName createDocumentDirectoryName(final String documentId) {
    return ObjectName.of(DOCUMENTPREFIX, documentId);
  }

  protected ObjectName createDocumentObjectName(final String documentId) {
    return createDocumentDirectoryName(documentId).append("document.json");
  }

  /**
   * Delete the document identified by the given id.
   *
   * @param documentId the id of the document to delete
   */
  private void doDeleteDocument(final String documentId) {
    objectStore.delete(createDocumentDirectoryName(documentId));
  }
}
