package com.neverpile.eureka.api.index;

import java.util.List;

import com.neverpile.eureka.model.Document;
import com.neverpile.common.condition.Condition;

/**
 * Service to run queries against the index. These {@link DocumentQuery}s allow the use of complex combinations of
 * {@link Condition}s and filters to return a List of matching {@link Document}s.
 */
public interface QueryService {

  /**
   * Uses a Query object to list all matching documents. The list may be empty if no document is
   * matching.
   * 
   * @param searchQuery the query to use
   * @return List of matching documents
   */
  List<Document> queryDocuments(DocumentQuery searchQuery);
}
