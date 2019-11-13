package com.neverpile.eureka.api;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.neverpile.eureka.api.exception.VersionMismatchException;
import com.neverpile.eureka.model.Document;

/**
 * The document service provides access to documents (create/read/update/delete) within a eureka
 * instance. Documents are fundamentally identified by their unique document-id and are represented
 * by the {@link Document} model class.
 */
public interface DocumentService {
  public class DocumentServiceException extends NeverpileException {
    private static final long serialVersionUID = 1L;

    public DocumentServiceException(final String message, final Throwable cause) {
      this(message);
    }

    public DocumentServiceException(final String message) {
      super(message);
    }
  }

  public class DocumentAlreadyExistsException extends DocumentServiceException {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final Document document;

    public DocumentAlreadyExistsException(final Document document) {
      super("The document with the given id already exists");
      this.document = document;
    }
  }

  /**
   * Return the document uniquely identified by the given document id.
   *
   * @param documentId the document's unique id
   * @return the optional document
   * @throws DocumentServiceException in case of other failures
   */
  Optional<Document> getDocument(String documentId);

  /**
   * Create a new document owned by the given tenant, belonging to the given collection based on the
   * passed document instance. The given document's id may be set to a non-<code>null</code> value
   * in which case the id must not already be associated with an existing document. If the id is
   * <code>null</code>, a new, unique id will be generated during the operation.
   * <p>
   * During the creation process, some parts of the document's metadata may be modified or updated,
   * e.g. {@link Document#getDocumentId()}, {@link Document#getDateCreated()} etc. The returned
   * document instance will reflect those changes.
   *
   * @param document the document to be created
   * @return the new document
   * @throws DocumentServiceException in case of other failures
   */
  Document createDocument(Document document);

  /**
   * Deletes a specific document. The documentId serves as identifier of the document
   *
   * @param documentID the document's unique id
   * @return Returned true if the document was successfully deleted
   */
  boolean deleteDocument(String documentID);

  /**
   * Updates an existing document with the given {@link Document}'s data. Return <code>null</code>
   * if the document can not be found.
   *
   * @param deltaDocument the document instance containing the new/updated data
   * @return updated document or <code>null</code>
   * @throws VersionMismatchException if an attempt is made to update a document version that is
   *           not/no longer the current version
   * @throws DocumentServiceException in case of other failures
   */
  Optional<Document> update(Document deltaDocument);

  /**
   * This method checks whether a document with the given documentID exists. If this is not the
   * case, or if the method has not yet been fully implemented, it returns false by default.
   *
   * @param documentId the document's unique id
   * @return false if the document does not exist or the method has not yet been fully implemented
   */
  boolean documentExists(String documentId);

  /**
   * Returns all documentIds as a stream.
   *
   * @return String stream of documentIds
   * @throws DocumentServiceException in case of other failures
   */
  Stream<String> getAllDocumentIds();

  /**
   * Returns Documents to all given ids if present. The list may be empty if no document of the
   * given ids exists.
   * 
   * @param ids the list ids for which the document shall be returned.
   * @return List of matching documents
   * @throws DocumentServiceException in case of other failures
   */
  List<Document> getDocuments(List<String> ids);
}
