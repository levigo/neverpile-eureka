package com.neverpile.eureka.search.elastic;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.neverpile.common.opentelemetry.TraceInvocation;
import com.neverpile.eureka.api.index.IndexMaintenanceService;
import com.neverpile.eureka.model.Document;

@Service
public class ElasticsearchIndexMaintenanceService implements IndexMaintenanceService {

  @Autowired
  private ElasticsearchDocumentIndex index;

  @PostConstruct
  public void init() {
    index.ensureIndexUpToDateOrRebuildInProgress();
  }

  @Override
  @TraceInvocation
  public void indexDocument(final Document doc) {
    index.addDocument(doc, ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);
  }

  @Override
  @TraceInvocation
  public void updateDocument(final Document doc) {
    index.updateDocument(doc, ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);
  }

  @Override
  @TraceInvocation
  public void deleteDocument(final String documentId) {
    index.deleteDocument(documentId, ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);
  }

  @Override
  public void hardResetIndex() {
    index.hardResetIndex();
  }

  @Override
  public void rebuildIndex() {
    index.rebuildIndex();
  }

}