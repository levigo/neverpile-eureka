package com.neverpile.eureka.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.neverpile.eureka.model.Document;

/**
 * This interface extends the basic document service with capabilities to manage multiple versions
 * of a particular document. It is based on the premise, that all mutations on a document (be it
 * facet data, content, etc.) will never replace the old state but always create a new version.
 * Changes between versions may be determined by looking at two or more versions of the document.
 * <p>
 * Versions are identified by a high resolution (nanosecond) timestamp represented as an
 * {@link Instant}.
 */
public interface MultiVersioningDocumentService extends DocumentService {
  /**
   * Return the current version of the document uniquely identified by the given document id.
   * <p>
   * Note: this API is identical to the overridden {@link DocumentService#getDocument(String)}
   * except for the method comment clarifying that the <em>current</em> version will be returned.
   *
   * @param documentId the document's unique id
   * @return the optional document
   * @throws DocumentServiceException in case of other failures
   */
  Optional<Document> getDocument(String documentId);

  /**
   * Return the version specified by the given timestamp of the document uniquely identified the
   * given document id.
   *
   * @param documentId       the document's unique id
   * @param versionTimestamp the timestamp of the version to retrieve
   * @return the optional document
   * @throws DocumentServiceException in case of other failures
   */
  Optional<Document> getDocumentVersion(String documentId, Instant versionTimestamp);

  /**
   * Return the timestamps of all versions of the document identified by the given id. Return an
   * empty list if the document cannot be found.
   *
   * @param documentId the document id for which to fetch all version timestamps
   * @return the list of versions
   */
  List<Instant> getVersions(String documentId);
}
