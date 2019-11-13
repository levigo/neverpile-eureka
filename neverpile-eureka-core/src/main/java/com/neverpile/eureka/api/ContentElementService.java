package com.neverpile.eureka.api;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;

import com.neverpile.eureka.api.ObjectStoreService.StoreObject;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.ObjectName;

public interface ContentElementService {
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
   * Generates a ContentElentKey from the supplied documentId, as well as the ContentId.
   * 
   * @param documentId ID of the document
   * @param contentId ID of the content element
   * @return {@link ObjectName} ObjectName
   */
  ObjectName createObjectName(final String documentId, final String contentId);
  
  StoreObject getContentElement(final String documentId, final String contentId);
 
  boolean deleteContentElement(String documentId, String contentId);
  
  boolean checkContentExist(final String documentId, final String contentId);
  
  ContentElement createContentElement(final String documentId, final String elementId, InputStream is,
      String name, String filename, String contentType, MessageDigest md, List<ContentElement> existingElements);
}
 
  