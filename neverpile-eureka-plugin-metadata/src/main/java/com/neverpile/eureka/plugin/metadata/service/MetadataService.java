package com.neverpile.eureka.plugin.metadata.service;

import java.util.Optional;

import com.neverpile.eureka.model.Document;

public interface MetadataService {
  /**
   * Retrieve the metadata associated with the given document. 
   * 
   * @param document the document for which to retrieve metadata
   * @return the metadata as an Optional
   */
  Optional<Metadata> get(Document document);

  /**
   * Store/update the metadata associated with the given document.
   * 
   * @param document the document
   * @param metadata the metadata to be stored/updated
   * @return the stored metadata
   */
  Metadata store(Document document, Metadata metadata);

  /**
   * Delete all metadata associated with the given document.
   * 
   * @param document the document
   */
  void delete(Document document);
}
