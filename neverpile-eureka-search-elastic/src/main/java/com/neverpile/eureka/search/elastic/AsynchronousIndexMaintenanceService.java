package com.neverpile.eureka.search.elastic;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.api.index.IndexMaintenanceService;
import com.neverpile.eureka.api.index.Schema;
import com.neverpile.eureka.event.AggregatedUpdateEvent;
import com.neverpile.eureka.event.CreateEvent;
import com.neverpile.eureka.event.DeleteEvent;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.tasks.DistributedPersistentQueueType;
import com.neverpile.eureka.tasks.TaskQueue;
import com.neverpile.eureka.tasks.TaskQueue.ProcessElement;

@Service
public class AsynchronousIndexMaintenanceService implements IndexMaintenanceService {
  private enum EventType {
    CREATE, UPDATE, DELETE
  }

  private final static Logger LOGGER = LoggerFactory.getLogger(AsynchronousIndexMaintenanceService.class);

  @Autowired
  private ElasticsearchDocumentIndex index;

  @Autowired
  DocumentService documentService;

  @DistributedPersistentQueueType("neverpile-index-maintenance")
  TaskQueue<EventType> indexMaintenanceQueue;

  private final AtomicBoolean dbJobActive = new AtomicBoolean(false);
  private final AtomicBoolean rebuildActive = new AtomicBoolean(false);

  class ElasticQueueUpdateListener implements TaskQueue.QueueListener<EventType> {
    @Override
    public void notifyUpdate() {
      if (!dbJobActive.getAndSet(true)) {
        getDBJobFromQueue();
      }
    }
  }

  private void getDBJobFromQueue() {
    ProcessElement<EventType> job = indexMaintenanceQueue.getElementToProcess();
    if (null == job) {
      dbJobActive.set(false);
      if (rebuildActive.getAndSet(false)) {
        finalizeIndexRebuild();
      }
      return;
    }
    if (job.getValue() == EventType.DELETE) {
      doDeleteDocument(job.getKey());
      getDBJobFromQueue();
      return;
    }

    Document doc = documentService.getDocument(job.getKey()).orElse(null);
    if (null == doc) {
      LOGGER.error("Document Not Found: {}", job.getKey());
      indexMaintenanceQueue.removeProcessedElement(job.getKey());
      getDBJobFromQueue();
      return;
    }

    if (job.getValue() == EventType.CREATE) {
      doIndexDocument(doc);
      getDBJobFromQueue();
      return;
    }
    if (job.getValue() == EventType.UPDATE) {
      doUpdateDocument(doc);
      getDBJobFromQueue();
      return;
    }
  }

  @PostConstruct
  public void init() throws IOException {
    ensureIndexUpToDateOrRebuildInProgress();

    indexMaintenanceQueue.registerListener(new ElasticQueueUpdateListener());
  }

  @EventListener
  public void onApplicationEvent(final CreateEvent event) {
    indexMaintenanceQueue.putInQueue(event.getDocument().getDocumentId(), EventType.CREATE);
  }

  @EventListener
  public void onApplicationEvent(final AggregatedUpdateEvent event) {
    indexMaintenanceQueue.putInQueue(event.getDocument().getDocumentId(), EventType.UPDATE);
  }

  @EventListener
  public void onApplicationEvent(final DeleteEvent event) {
    indexMaintenanceQueue.putInQueue(event.getDocumentId(), EventType.DELETE);
  }

  @Override
  public void indexDocument(final Document doc) {
    indexMaintenanceQueue.putInQueue(doc.getDocumentId(), EventType.CREATE);
  }

  @Override
  public void updateDocument(final Document doc) {
    indexMaintenanceQueue.putInQueue(doc.getDocumentId(), EventType.UPDATE);
  }

  @Override
  public void deleteDocument(final String documentId) {
    indexMaintenanceQueue.putInQueue(documentId, EventType.DELETE);
  }

  private void doIndexDocument(final Document doc) {
    index.addDocument(doc, ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);
  }


  private void doUpdateDocument(final Document doc) {
    index.updateDocument(doc, ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);
  }


  private void doDeleteDocument(final String documentId) {
    index.deleteDocument(documentId, ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);
  }

  @Override
  public void hardResetIndex() {
    index.hardResetIndex();
  }

  @Override
  public void rebuildIndex() {
    LOGGER.info("Index rebuild started.");
    rebuildActive.set(true);
    try {
      String inProgressIndexName = index.createIndex();
      index.setAliasForIndex(inProgressIndexName, ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);

      LOGGER.info("New index created.");

      Stream<String> stream = documentService.getAllDocumentIds();
      long enqueuedElements = stream
          .filter(Objects::nonNull)
          .peek(documentId -> indexMaintenanceQueue.putInQueue(documentId, EventType.CREATE)).count();
      LOGGER.info("Index rebuild enqueued {} Elements.", enqueuedElements);

      if (enqueuedElements < 1) {
        rebuildActive.set(false);
        finalizeIndexRebuild();
      }

    } catch (Exception e) {
      LOGGER.error("Failed to (re)bulild index.", e);
    }
  }

  private void finalizeIndexRebuild() {
    try {
      String obsoleteIndexName = index.getIndexNameFromAlias(ElasticsearchDocumentIndex.INDEX_ALIAS_READ);
      String newIndexName = index.getIndexNameFromAlias(ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);

      // switch active index
      index.setAliasForIndex(newIndexName, ElasticsearchDocumentIndex.INDEX_ALIAS_READ);

      // delete old one
      if (null != obsoleteIndexName) {
        index.deleteIndex(obsoleteIndexName);
      }
    } catch (IOException e) {
      LOGGER.error("Error while finalizing index rebuild!");
    }
  }

  private void ensureIndexUpToDateOrRebuildInProgress() throws IOException {
    Schema schema = index.createIndexSchema();
    JsonNode expectedMapping = index.schemaToMapping(schema);

    try {
      JsonNode currentReadMapping = index.getCurrentMapping(ElasticsearchDocumentIndex.INDEX_ALIAS_READ);

      if (expectedMapping.equals(currentReadMapping)) {
        LOGGER.info("Current schema mapping is up to date");
        return;
      }

      JsonNode currentWriteMapping = index.getCurrentMapping(ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);

      if (expectedMapping.equals(currentWriteMapping)) {
        LOGGER.info("Current schema mapping is outdated, but rebuild seems to be in progress");
        return;
      }
    } catch (IOException e) {
      LOGGER.info("Index not found, creating new index...");
    }

    // needs rebuild
    rebuildIndex();
  }
}