package com.neverpile.eureka.api;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;

import javax.ws.rs.core.MediaType;

import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.ObjectName;

/**
 * Service to provide access to create/read/delete operations for {@link ContentElement}s within a eureka instance.
 * {@link ContentElement}s are identified by their unique {@link ContentElement#id} within a single {@link Document}
 * and are represented by the {@link ContentElement} model class.
 */
public interface ContentElementService {
  /**
   * Generic exception thrown when an error occurred while executing an operation on {@link ContentElement}s.
   */
  public class ContentElementException extends NeverpileException {
    private static final long serialVersionUID = 1L;

    public ContentElementException(final String message, final Throwable cause) {
      super(message, cause);
    }

    public ContentElementException(final String message) {
      super(message);
    }
  }

  /**
   * Generates a {@link ObjectName} from the supplied {@link Document#documentId}, as well as the {@link ContentElement#id} .
   *
   * @param documentId {@link Document#documentId} of the associated
   *                   {@link Document}.
   * @param contentId  {@link ContentElement#id} of the {@link ContentElement} to be created.
   * @return {@link ObjectName} of the {@link ContentElement}.
   */
  ObjectName createObjectName(final String documentId, final String contentId);

  /**
   * Get a {@link ContentElement} as a generic {@link InputStream}.
   *
   * @param documentId {@link Document#documentId} of the associated
   *                   {@link Document}.
   * @param contentId  {@link ContentElement#id} of the requested {@link ContentElement}.
   * @return {@link InputStream} of the {@link ContentElement}
   */
  InputStream getContentElement(final String documentId, final String contentId);

  /**
   * Delete an existing {@link ContentElement}.
   *
   * @param documentId {@link Document#documentId} of the associated
   *                   {@link Document}.
   * @param contentId  {@link ContentElement#id} of the {@link ContentElement} to delete.
   * @return {@code true} if the deletion was successful - {@code false} otherwise.
   */
  boolean deleteContentElement(String documentId, String contentId);

  /**
   * Checks if a {@link ContentElement} already exists.
   *
   * @param documentId {@link Document#documentId} of the associated
   *                   {@link Document}.
   * @param contentId  {@link ContentElement#id} of the {@link ContentElement} to check.
   * @return {@code true} if the {@link ContentElement} exists - {@code false} otherwise.
   */
  boolean checkContentExist(final String documentId, final String contentId);

  /**
   * Create a new {@link ContentElement} for a given document.
   *
   * @param documentId       {@link Document#documentId} of the associated
   *                         {@link Document}.
   * @param contentId        {@link ContentElement#id} of the {@link ContentElement} to check. If null the ID will be generated.
   * @param is               InputStream with the actual data.
   * @param role             {@link ContentElement#role} of the {@link ContentElement}.
   * @param filename         {@link ContentElement#fileName} of the {@link ContentElement}.
   * @param contentType      String representation of the {@link MediaType} for {@link ContentElement#type} of the {@link ContentElement}.
   * @param md               {@link MessageDigest} for the data.
   * @param existingElements Already existing {@link ContentElement}s within the document the new element will be appended to.
   * @return the newly created {@link ContentElement}
   */
  ContentElement createContentElement(final String documentId, final String contentId, InputStream is, String role,
      String filename, String contentType, MessageDigest md, List<ContentElement> existingElements);
}
 
  
