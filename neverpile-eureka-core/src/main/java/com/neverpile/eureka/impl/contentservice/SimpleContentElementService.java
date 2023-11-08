package com.neverpile.eureka.impl.contentservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.List;

import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.neverpile.eureka.api.ContentElementIdGenerationStrategy;
import com.neverpile.eureka.api.ContentElementService;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.ObjectStoreService.StoreObject;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.impl.documentservice.DefaultDocumentService;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.model.HashAlgorithm;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.util.SizeTrackingOutputStream;

@Component
public class SimpleContentElementService implements ContentElementService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDocumentService.class);

  @Autowired
  EventPublisher eventAPublisher;

  @Autowired
  private ObjectStoreService objectStore;

  @Autowired
  private ContentElementIdGenerationStrategy idGenerationStrategy;

  /**
   * Generates a ContentElementKey from the supplied documentId, as well as the ContentId.
   *
   * @param documentId the id of the document
   * @param contentId id of the content element
   * @return {@link ObjectName} ObjectName
   */
  @Override
  public ObjectName createObjectName(final String documentId, final String contentId) {
    return ObjectName.of("document", documentId, contentId);
  }

  @Override
  public InputStream getContentElement(final String documentId, final String contentId) {
    StoreObject storeObject = objectStore.get(createObjectName(documentId, contentId));
    return storeObject == null ? null : storeObject.getInputStream();
  }

  @Override
  public boolean deleteContentElement(final String documentId, final String contentId) {
    ObjectName objectName = ObjectName.of("document", documentId, contentId);
    try {
      eventAPublisher.publishDeleteEvent(documentId);
      objectStore.delete(objectName);
      return true;
    } catch (ContentElementException e) {
      LOGGER.error("Failed to delete document @{}. - Conflict detected", objectName, e);
      return false;
    }
  }

  @Override
  public boolean checkContentExist(final String documentId, final String contentId) {
    return objectStore.checkObjectExists(createObjectName(documentId, contentId));
  }

  @Override
  public ContentElement createContentElement(final String documentId, final String elementId, final InputStream is,
      final String role, final String filename, final String contentType, final MessageDigest elementDigest, final List<ContentElement> existingElements) {
    try {
      File tempFile = File.createTempFile("neverpile-eureka-", null);
      try {
        // write source to temporary file, determining digest and length on the fly
        Digest digest;
        long length;

        try (InputStream source = is) {
          try (FileOutputStream tmpOut = new FileOutputStream(tempFile)) {
            SizeTrackingOutputStream sizeTrackingOutputStream = new SizeTrackingOutputStream(tmpOut);
            DigestOutputStream digestOutputStream = new DigestOutputStream(sizeTrackingOutputStream, elementDigest);

            StreamUtils.copy(source, digestOutputStream);

            digest = new Digest();
            digest.setAlgorithm(HashAlgorithm.fromValue(elementDigest.getAlgorithm()));
            digest.setBytes(digestOutputStream.getMessageDigest().digest());

            length = sizeTrackingOutputStream.getBytesWritten();
          }
        }

        // Set Id
        String contentElementId = null;
        if (null != elementId) {
          idGenerationStrategy.validateContentId(elementId, existingElements, digest);
          contentElementId = elementId;
        } else {
          contentElementId = idGenerationStrategy.createContentId(existingElements, digest);
        }

        // save Content via ObjectStore
        try (InputStream tmpIn = new FileInputStream(tempFile)) {
          objectStore.put(ObjectName.of("document", documentId, contentElementId), ObjectStoreService.NEW_VERSION, tmpIn, length);
        }

        // Create ContentElementDto
        ContentElement newContentDto = new ContentElement();

        newContentDto.setContentElementId(contentElementId);
        newContentDto.setDigest(digest);
        newContentDto.setEncryption(EncryptionType.SHARED);
        newContentDto.setRole(role);
        newContentDto.setType(null != contentType
            ? MediaType.valueOf(contentType)
            : MediaType.APPLICATION_OCTET_STREAM_TYPE);
        newContentDto.setFileName(filename);
        newContentDto.setLength(length);

        return newContentDto;
      } finally {
        tempFile.delete();
      }
    } catch (IOException e) {
      throw new ContentElementException("Can't create content element", e);
    }
  }
}

