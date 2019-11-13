package com.neverpile.eureka.api.index;

import java.util.List;

import com.neverpile.eureka.model.Document;

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
