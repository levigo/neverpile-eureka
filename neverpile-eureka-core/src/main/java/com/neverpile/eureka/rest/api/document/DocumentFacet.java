package com.neverpile.eureka.rest.api.document;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.neverpile.common.authorization.api.AuthorizationContext;
import com.neverpile.eureka.api.DocumentAssociatedEntityStore;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.index.Schema;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.tx.wal.WriteAheadLog;

/**
 * <h3 id="tx">Transaction management</h3> Facets must make sure that any changes they make are
 * rolled back in case a mutation operation fails later on. <br>
 * Implementations of {@link ObjectStoreService} and {@link DocumentAssociatedEntityStore} are required to
 * take appropriate measures to ensure transaction management, e.g. by using write-ahead-logging.
 * Thus, facets using just those interfaces are automatically safe. Additionally, facets using other
 * persistence mechanisms that integrate with spring's transaction support are also automatically
 * covered. <br>
 * In all other cases, facets are responsible themselves, but may use facilities like the
 * {@link WriteAheadLog} or the {@link TransactionSynchronizationManager} for synchronization
 * purposes.
 *
 * @param <V> the type of value associated with the facet
 */
public interface DocumentFacet<V> {
  public static class ConstraintViolation {
    public static <V> Set<ConstraintViolation> none() {
      return Collections.emptySet();
    }

    public static <V> Set<ConstraintViolation> fail(final DocumentFacet<?> facet, final String... reason) {
      return Arrays.stream(reason).map(r -> new ConstraintViolation(facet, r)).collect(Collectors.toSet());
    }

    private final DocumentFacet<?> facet;

    private final String reason;

    public ConstraintViolation(final DocumentFacet<?> facet, final String message) {
      this.facet = facet;
      this.reason = message;
    }

    public String getReason() {
      return reason;
    }

    public DocumentFacet<?> getFacet() {
      return facet;
    }

    @Override
    public String toString() {
      return "ConstraintViolation{" + "facet=" + facet + ", reason='" + reason + '\'' + '}';
    }
  }

  String getName();

  /**
   * Whether to include this facet by default upon retrieve operations.
   * 
   * @return <code>true</code> if this facet is to be included by default
   */
  default boolean includeByDefault() {
    return true;
  }
  
  JavaType getValueType(TypeFactory f);

  /**
   * Called during a document retrieval which occurs on GET, but also at the end of mutation
   * operations. Typically, a facet will populate the response DTO within this hook method.
   * 
   * @param document current/final the state of the to be retrieved
   * @param responseDto the response DTO to be populated
   */
  default void onRetrieve(final Document document, final DocumentDto responseDto) {
    // do nothing
  }

  /**
   * Called during document creation to give facets a chance to validate the creation request.
   * <p>
   * Facets will typically verify the data provided in the request and return a set of appropriate
   * {@link ConstraintViolation}s (using {@link ConstraintViolation#fail(DocumentFacet, String...)})
   * if the creation should be denied. Otherwise they must return an empty set of violations by
   * returning {@link ConstraintViolation#none()}.
   * <p>
   * 
   * @param requestDto the create request DTO
   * @return an empty set of violations to agree with the request, a non-empty set to deny it.
   */
  default Set<ConstraintViolation> validateCreate(final DocumentDto requestDto) {
    return ConstraintViolation.none(); // by default, we agree with everything
  }

  /**
   * Called during document creation before the new document is persisted.
   * <p>
   * Facets will typically take measures to persist the data associated with the document facet
   * using whatever means. In order to persist data along with the core document data itself see
   * {@link DocumentAssociatedEntityStore}. In order to persist data into separate objects see
   * {@link ObjectStoreService}.
   * 
   * @param toBeCreated the document to be persisted
   * @param requestDto the create request DTO
   * @see <a href="#tx">notes on transaction management in class javadoc</a>
   */
  default void beforeCreate(final Document toBeCreated, final DocumentDto requestDto) {
    // do nothing
  }

  /**
   * Called during document creation after the new document has been persisted.
   * <p>
   * Facets will typically populate the response DTO with the data that has been persisted so that
   * the response DTO reflects the correct, persisted state.
   * 
   * @param persisted the document that has been persisted
   * @param responseDto the create response DTO
   */
  default void afterCreate(final Document persisted, final DocumentDto responseDto) {
    onRetrieve(persisted, responseDto); // by default, just retrieve the current state
  }

  /**
   * Called during document update to give facets a chance to validate the update request.
   * <p>
   * Facets will typically verify the data provided in the request and return a set of appropriate
   * {@link ConstraintViolation}s (using {@link ConstraintViolation#fail(DocumentFacet, String...)})
   * if the update should be denied. Otherwise they must return an empty set of violations by
   * returning {@link ConstraintViolation#none()}.
   * 
   * @param currentDocument the current, persistent state of the document
   * @param requestDto the update request DTO
   * @return an empty set of violations to agree with the request, a non-empty set to deny it.
   */
  default Set<ConstraintViolation> validateUpdate(final Document currentDocument, final DocumentDto requestDto) {
    return ConstraintViolation.none(); // by default, we agree everything
  }

  /**
   * Called during a document update before the update is persisted.
   * 
   * @param currentDocument the current, persistent state of the document
   * @param updatedDocument the new, updated state to be persisted
   * @param updateDto the update request DTO
   * @see <a href="#tx">notes on transaction management in class javadoc</a>
   */
  default void beforeUpdate(final Document currentDocument, final Document updatedDocument,
      final DocumentDto updateDto) {
    // do nothing
  }

  /**
   * Called during document update after the new document state has been persisted.
   * <p>
   * Facets will typically populate the response DTO with the data that has been persisted so that
   * the response DTO reflects the correct, persisted state.
   * 
   * @param persisted the document that has been persisted
   * @param responseDto the create response DTO
   */
  default void afterUpdate(final Document persisted, final DocumentDto responseDto) {
    onRetrieve(persisted, responseDto); // by default, just retrieve the current state
  }

  /**
   * Called during document deletion to give facets a chance to validate the delete request.
   * <p>
   * Facets will typically verify the current state and other circumstances and return a set of
   * appropriate {@link ConstraintViolation}s (using
   * {@link ConstraintViolation#fail(DocumentFacet, String...)}) if the deletion should be denied.
   * Otherwise they must return an empty set of violations by returning
   * {@link ConstraintViolation#none()}.
   * 
   * @param currentDocument the current, persistent state of the document
   * @return an empty set of violations to agree with the request, a non-empty set to deny it.
   */
  default Set<ConstraintViolation> validateDelete(final Document currentDocument) {
    return ConstraintViolation.none(); // by default, we agree everything
  }

  /**
   * Called during a document deletion before the deletion is persisted.
   * <p>
   * Notes on persistence: facets using just the {@link DocumentAssociatedEntityStore} are not required to
   * take any steps to delete their data as it will automatically be removed during document
   * deletion. Facets using other mechanisms are must take appropriate measures themselves.
   * 
   * @param currentDocument the current, persistent state of the document
   * @see <a href="#tx">notes on transaction management in class javadoc</a>
   */
  default void onDelete(final Document currentDocument) {
    // do nothing
  }

  default Schema getIndexSchema() {
    return null;
  }

  default JsonNode getIndexData(final Document document) {
    return null;
  }
  
  default AuthorizationContext getAuthorizationContextContribution(final Document document) {
    return null;
  }
}
