package com.neverpile.eureka.search.elastic;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.neverpile.eureka.api.index.IndexMaintenanceService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.tracing.NewSpan;

@Service
public class ElasticsearchIndexMaintenanceService implements IndexMaintenanceService {

  @Autowired
  private ElasticsearchDocumentIndex index;

  @PostConstruct
  public void init() {
    index.ensureIndexUpToDateOrRebuildInProgress();
  }

  @Override
  @NewSpan
  public void indexDocument(final Document doc) {
    index.addDocument(doc, ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);
  }

  @Override
  @NewSpan
  public void updateDocument(final Document doc) {
    index.updateDocument(doc, ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE);
  }

  @Override
  @NewSpan
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