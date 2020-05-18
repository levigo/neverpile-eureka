package com.neverpile.eureka.impl.documentservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.common.opentracing.TraceInvocation;
import com.neverpile.common.util.VisibleForTesting;
import com.neverpile.eureka.api.DocumentAssociatedEntityStore;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.ObjectStoreService.ObjectNotFoundException;
import com.neverpile.eureka.api.ObjectStoreService.ObjectStoreException;
import com.neverpile.eureka.api.ObjectStoreService.StoreObject;
import com.neverpile.eureka.api.exception.VersionMismatchException;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.rest.api.exception.NotFoundException;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;
import com.neverpile.eureka.util.CompositeKey;

/**
 * An implementation of {@link MultiVersioningDocumentService} which stores all document metadata
 * within an object store. It implements multi-versioning, treating the object store as append-only.
 * <p>
 * Optimistic concurrency control is implemented based on object store object version tracking as
 * well as document version timestamp checking.
 */
public class DefaultMultiVersioningDocumentService
    implements
      MultiVersioningDocumentService,
      DocumentAssociatedEntityStore {
  private static final String META_SUB_PREFIX = "meta";

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMultiVersioningDocumentService.class);

  @Autowired
  EventPublisher eventPublisher;

  @Autowired
  private ObjectStoreService objectStore;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ModelMapper modelMapper;

  @Autowired
  private ClusterLockFactory lock;

  @Autowired
  private Clock clock;
  
  @VisibleForTesting
  public static final String DOCUMENT_PREFIX = "document";

  @VisibleForTesting
  public static final DateTimeFormatter VERSION_FORMATTER = DateTimeFormatter.ISO_INSTANT;

  private static final Instant NEW_VERSION_MARKER = Instant.ofEpochMilli(Long.MAX_VALUE);

  private enum State {
    Unmodified, Created, Deleted, Modified;
  }

  private class TransactionalDocument {
    Map<String, JsonNode> sidecar = new HashMap<>();
    Instant initialTimestamp;
    DocumentPdo document;
    State state = State.Unmodified;

    public TransactionalDocument(final DocumentPdo doc) {
      this.document = doc;
      this.initialTimestamp = doc != null ? doc.getVersionTimestamp() : null;
      this.sidecar = doc != null ? doc.getAssociatedFacetData() : new HashMap<>();
    }

    public TransactionalDocument(final Instant version) {
      initialTimestamp = version;
    }

    public void update(final DocumentPdo updated) {
      document = updated;
    }
  }

  private class EntityRegistry {
    private final boolean mutable;

    final Map<CompositeKey, TransactionalDocument> documents = new HashMap<>();

    public EntityRegistry(final boolean mutable) {
      this.mutable = mutable;
    }

    public TransactionalDocument document(final String documentId, Instant versionTimestamp) {
      if (null == versionTimestamp) {
        // retrieve latest version
        Optional<Instant> ts = getCurrentVersionTimestamp(documentId);
        if (ts.isPresent()) {
          versionTimestamp = ts.get();
          // fall through
        } else {
          // document does not (yet) exist
          return documents.computeIfAbsent(new CompositeKey(documentId, NEW_VERSION_MARKER),
              k -> new TransactionalDocument(NEW_VERSION_MARKER));
        }
      }

      // retrieve a particular version and cache it
      Instant effectiveTimestamp = versionTimestamp;
      CompositeKey key = new CompositeKey(documentId, effectiveTimestamp);
      return documents.computeIfAbsent(key,
          k -> new TransactionalDocument(doRetrieveDocument(documentId, effectiveTimestamp)));
    }

    public Instant create(final DocumentPdo document) {
      if (!mutable)
        throw new IllegalStateException("Mutations not supported outside transactions");

      // there should already be a transactional document, but with an empty document
      TransactionalDocument txd = document(document.getDocumentId(), document.getVersionTimestamp());
      if (txd.document != null)
        throw new IllegalStateException("Document already created");

      // set version timestamp for creation
      document.setVersionTimestamp(clock.instant());

      txd.update(document);
      txd.state = State.Created;

      // store with new version timestamp as "alias"
      documents.put(new CompositeKey(document.getDocumentId(), document.getVersionTimestamp()), txd);
      
      return document.getVersionTimestamp();
    }

    public void delete(final String id) {
      if (!mutable)
        throw new IllegalStateException("Mutations not supported outside transactions");

      TransactionalDocument current = document(id, null);
      if (null == current.document)
        throw new NotFoundException("Document " + id + " to be deleted not found");

      // prepare a deletion marker which retains the current properties but flags it as deleted
      DocumentPdo deletionMarker = modelMapper.map(current.document, DocumentPdo.class);
      deletionMarker.setDeleted(true);
      deletionMarker.setDocumentId(id);
      deletionMarker.setVersionTimestamp(clock.instant());

      current.state = State.Deleted;
      current.update(deletionMarker);
    }

    public DocumentPdo update(final DocumentPdo updated) {
      if (!mutable)
        throw new IllegalStateException("Mutations not supported outside transactions");

      String id = updated.getDocumentId();

      // fetch most recent version!
      TransactionalDocument current = document(id, null);
      DocumentPdo currentDocument = current.document;

      if (null == currentDocument)
        throw new NotFoundException("Document " + id + " to be updated not found");

      // load sidecar data if it is not yet loaded
      /*
       * Check version timestamps. We accept the update if either the updated timestamp is correct
       * or null, in which case we treat the update as non-version checking.
       */
      if (null != updated.getVersionTimestamp()
          && !currentDocument.getVersionTimestamp().equals(updated.getVersionTimestamp()))
        throw new VersionMismatchException("Document version timestamps do not match",
            currentDocument.getVersionTimestamp().toString(), updated.getVersionTimestamp().toString());

      // ok, we can go ahead with the update
      updated.setVersionTimestamp(clock.instant());

      current.state = State.Modified;
      current.update(updated);

      // index the new version as an "alias" to the original version
      documents.put(new CompositeKey(id, updated.getVersionTimestamp()), current);

      return updated;
    }

    public void flush() {
      // flush creates and updates
      documents.values().stream() //
          .distinct() //
          .filter(txd -> txd.state != State.Unmodified && txd.document != null)
          // update sidecar
          .peek(txd -> txd.document.setAssociatedFacetData(txd.sidecar)) //
          .forEach(txd -> {
            Document persisted = doPersistDocument(txd.document, txd.initialTimestamp);

            switch (txd.state){
              case Created :
                eventPublisher.publishCreateEvent(persisted);
                break;
              case Deleted :
                eventPublisher.publishDeleteEvent(persisted.getDocumentId());
                break;
              case Modified :
                eventPublisher.publishUpdateEvent(persisted);
                break;
              default :
                // nothing to do
                break;
            }
          });
    }

    public void markAsModified(final Document document) {
      if (!mutable)
        throw new IllegalStateException("Mutations not supported outside transactions");

      document(document.getDocumentId(), document.getVersionTimestamp()).state = State.Modified;
    }

    public Map<String, JsonNode> sidecar(final Document document) {
      return document(document.getDocumentId(), document.getVersionTimestamp()).sidecar;
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
      TransactionSynchronizationManager.unbindResource(DefaultMultiVersioningDocumentService.this);
    }

    @Override
    public void resume() {
      TransactionSynchronizationManager.bindResource(DefaultMultiVersioningDocumentService.this, this.registry);
    }

    @Override
    public void beforeCommit(final boolean readOnly) {
      registry.flush();
    }

    @Override
    public void afterCompletion(final int status) {
      TransactionSynchronizationManager.unbindResourceIfPossible(DefaultMultiVersioningDocumentService.this);
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
    // Go through tx registry for current version
    DocumentPdo doc = txEntityRegistry().document(documentId, null).document;

    // we verify the deleted-status only upon retrieval of the current version
    if (doc != null && doc.isDeleted()) {
      throw new NotFoundException("Document has been deleted");
    }

    return Optional.ofNullable(doc);
  }

  @Override
  @CacheEvict(cacheNames = "documentVersions", key = "#document.documentId")
  @TraceInvocation
  public Document createDocument(final Document document) {
    if (!getVersions(document.getDocumentId()).isEmpty())
      throw new DocumentAlreadyExistsException(document);

    if (txEntityRegistry().document(document.getDocumentId(), document.getVersionTimestamp()).document != null)
      throw new DocumentAlreadyExistsException(document);

    Instant versionTimestamp = txEntityRegistry().create(modelMapper.map(document, DocumentPdo.class));
    
    document.setVersionTimestamp(versionTimestamp);

    return document;
  }

  @Override
  @CacheEvict(cacheNames = "documentVersions", key = "#documentId")
  @TraceInvocation
  public boolean deleteDocument(final String documentId) {
    txEntityRegistry().delete(documentId);
    return true;
  }

  @Override
  @CacheEvict(cacheNames = "documentVersions", key = "#document.documentId")
  @TraceInvocation
  public Optional<Document> update(final Document document) {
    return Optional.of(txEntityRegistry().update(modelMapper.map(document, DocumentPdo.class)));
  }

  @Override
  @TraceInvocation
  public boolean documentExists(final String documentId) {
    return !getVersions(documentId).isEmpty();
  }

  @Override
  @TraceInvocation
  public Stream<String> getAllDocumentIds() {
    // @formatter:off
    // Second part of ObjectName is documentId. See 'createDocumentDirectoryName()'.
    return objectStore
        .list(ObjectName.of(DOCUMENT_PREFIX))
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
    txEntityRegistry().sidecar(document).put(key, value);
    txEntityRegistry().markAsModified(document);
  }

  @Override
  @TraceInvocation
  public Optional<JsonNode> retrieve(final Document document, final String key) {
    return Optional.ofNullable(txEntityRegistry().sidecar(document).get(key));
  }

  @Override
  @TraceInvocation
  public void delete(final Document document, final String key) {
    txEntityRegistry().sidecar(document).remove(key);
    txEntityRegistry().markAsModified(document);
  }

  private Optional<Instant> getCurrentVersionTimestamp(final String documentId) {
    List<Instant> versions = getVersions(documentId);
    if (versions.isEmpty())
      return Optional.empty();

    return Optional.of(versions.get(versions.size() - 1));
  }

  /**
   * Perform the actual retrieval of a document based on a given id.
   * 
   * @param documentId the id of the document to retrieve
   * @param versionTimestamp timestamp acting as document version identifier
   * @return the document
   */
  private DocumentPdo doRetrieveDocument(final String documentId, final Instant versionTimestamp) {
    ObjectName objectName = createDocumentObjectName(documentId, versionTimestamp);
    StoreObject storedDocument = objectStore.get(objectName);

    if (null == storedDocument)
      return null;

    try {
      return objectMapper.readValue(storedDocument.getInputStream(), DocumentPdo.class);
    } catch (IOException e) {
      LOGGER.error("Failed to deserialize document @{}", objectName, e);
      throw new DocumentServiceException("Failed to retrieve document");
    }
  }

  /**
   * Persist changes to or create the given document.
   * 
   * @param document the document to persist
   * @param initialTimestamp timestamp acting as document version identifier
   * @return the document
   */
  private Document doPersistDocument(final Document document, final Instant initialTimestamp) {
    Lock writeLock = lock.writeLock("document-" + document.getDocumentId());
    writeLock.lock();
    try {
      try {
        // check for last minute version clash _within_ the write lock
        List<Instant> upToDateVersionList = doRetrieveVersionList(document.getDocumentId());

        // no initial timestamp or empty version list means: new document and thus no clash
        if (initialTimestamp != null //
            && !upToDateVersionList.isEmpty() //
            && upToDateVersionList.get(upToDateVersionList.size() - 1).isAfter(initialTimestamp)) {
          throw new VersionMismatchException("Mid-air-collision writing a document version",
              document.getVersionTimestamp().toString(),
              upToDateVersionList.get(upToDateVersionList.size() - 1).toString());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
        objectMapper.writeValue(baos, document);

        ObjectName objectName = createDocumentObjectName(document.getDocumentId(), document.getVersionTimestamp());
        try {
          objectStore.put(objectName, ObjectStoreService.NEW_VERSION, new ByteArrayInputStream(baos.toByteArray()),
              baos.size());

          return document;
        } catch (ObjectStoreException e) {
          LOGGER.error("Failed to store document @{}", objectName, e);
          throw new DocumentServiceException("Failed to store document");
        }
      } catch (IOException e) {
        LOGGER.error("Failed to serialize document", e);
        throw new DocumentServiceException("Failed to serialize document");
      }
    } finally {
      writeLock.unlock();
    }
  }

  protected ObjectName createDocumentDirectoryName(final String documentId) {
    return ObjectName.of(DOCUMENT_PREFIX, documentId, META_SUB_PREFIX);
  }

  protected ObjectName createDocumentObjectName(final String documentId, final Instant versionTimestamp) {
    return createDocumentDirectoryName(documentId).append(VERSION_FORMATTER.format(versionTimestamp));
  }

  @Override
  @TraceInvocation
  public Optional<Document> getDocumentVersion(final String documentId, final Instant versionTimestamp) {
    return Optional.ofNullable(txEntityRegistry().document(documentId, versionTimestamp).document);
  }

  /**
   * Retrieve the list of versions for a given document id. List will be empty if document does not
   * exist.
   * 
   * @param documentId the id of the document for which to retrieve the version list
   * @return the list of version timestamps - the empty list for documents that do not exist
   */
  @Override
  @Cacheable("documentVersions")
  @TraceInvocation
  public List<Instant> getVersions(final String documentId) {
    return doRetrieveVersionList(documentId);
  }

  /**
   * The actual work of retrieving the list of versions of a document. Not cached.
   * 
   * @param documentId the id of the document to retrieve the version list from
   * @return a list of versions to the corresponding document
   */
  private List<Instant> doRetrieveVersionList(final String documentId) {
    ObjectName versionsPrefix = createDocumentDirectoryName(documentId);

    try {
      // @formatter:off
      return objectStore
        .list(versionsPrefix)
        .filter(s -> s.getObjectName().length() > versionsPrefix.length())
        .map(s -> s.getObjectName().element(versionsPrefix.length()))
        .map(n -> Instant.from(VERSION_FORMATTER.parse(n)))
        .sorted()
        .collect(Collectors.toList());
      // @formatter:on
    } catch (ObjectNotFoundException e) {
      return Collections.emptyList();
    }
  }

}
