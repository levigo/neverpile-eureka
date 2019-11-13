package com.neverpile.eureka.api.index;

import com.neverpile.eureka.model.Document;

public interface IndexMaintenanceService {

  /**
   * Create new index entry for document
   *
   * @param doc new Document
   */
  void indexDocument(Document doc);

  /**
   * Update index information for document already in the index.
   *
   * @param doc Document with updated Information
   */
  void updateDocument(Document doc);

  /**
   * delete document from the index.
   *
   * @param documentId Id of the document to delete
   */
  void deleteDocument(String documentId);

  /**
   * Delete whole Index and reinitialized with current mapping information only.
   * <p>
   * <u>Caution</u>: All index data will be lost!
   */
  void hardResetIndex();

  /**
   * Rebuild the index from scratch. Information for the rebuild will be pulled directly from the database.
   * Current index will remain unchanged for all incoming requests until process is complete.
   * Updates to the index during the process will be included in the new index but wont be accessible
   * until rebuild is complete.
   */
  void rebuildIndex();
}
