package com.neverpile.eureka.plugin.audit.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.api.DocumentService.DocumentServiceException;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.ObjectStoreService.ObjectStoreException;
import com.neverpile.eureka.api.ObjectStoreService.StoreObject;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.service.AuditIdGenerationStrategy;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;
import com.neverpile.eureka.plugin.audit.verification.VerificationService;

public class DefaultAuditLogService implements AuditLogService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAuditLogService.class);

  @Autowired
  private VerificationService verificationService;

  @Autowired
  private ObjectStoreService objectStore;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AuditIdGenerationStrategy auditIdGenerationStrategy;

  @Override
  public List<AuditEvent> getEventLog(final String documentId) {
    Optional<List<AuditEvent>> eventList = findAuditLog(documentId);
    return eventList.orElseGet(ArrayList::new);
  }

  private Optional<List<AuditEvent>> findAuditLog(final String documentId) {
    ObjectName auditLogObjectName = createAuditLogObjectName(documentId);

    StoreObject storedDocumentAuditLog = objectStore.get(auditLogObjectName);

    if (null == storedDocumentAuditLog) {
      // Audit Log not found.
      return Optional.empty();
    }

    try {
      return Optional.ofNullable(objectMapper.readValue(storedDocumentAuditLog.getInputStream(),
          objectMapper.getTypeFactory().constructCollectionType(List.class, AuditEvent.class)));
    } catch (IOException e) {
      LOGGER.error("Failed to deserialize document auditLog @{}\n@{}", auditLogObjectName, e);
      throw new DocumentServiceException("Failed to retrieve document auditLog");
    }
  }

  @Override
  public void logEvent(final AuditEvent event) {
    saveToLog(event);
    verificationService.processEvent(event);
  }

  private void saveToLog(AuditEvent event) {
    ObjectName auditLogObjectName = createAuditLogObjectName(event.getDocumentId());

    StoreObject storedDocumentAuditLog = objectStore.get(auditLogObjectName);

    List<AuditEvent> eventList = new ArrayList<>();
    String version = ObjectStoreService.NEW_VERSION;

    if (null != storedDocumentAuditLog) {
      version = storedDocumentAuditLog.getVersion();
      try {
        eventList = objectMapper.readValue(storedDocumentAuditLog.getInputStream(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, AuditEvent.class));
      } catch (IOException e) {
        LOGGER.error("Failed to deserialize document auditLog @{}\n@{}", auditLogObjectName, e);
        throw new DocumentServiceException("Failed to retrieve document auditLog");
      }
    }

    eventList.add(event);

    try {
      // FIXME: replace BAISO/BAOS with buffer manager for efficiency
      ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
      objectMapper.writeValue(baos, eventList);

      try {
        objectStore.put(auditLogObjectName, version, new ByteArrayInputStream(baos.toByteArray()), baos.size());
      } catch (ObjectStoreException e) {
        LOGGER.error("Failed to store document auditLog @{}\n@{}", auditLogObjectName, e);
        throw new DocumentServiceException("Failed to store document auditLog");
      }
    } catch (IOException e) {
      LOGGER.error("Failed to serialize document auditLog", e);
      throw new DocumentServiceException("Failed to serialize document auditLog");
    }
  }

  @Override
  public AuditEvent getEvent(String auditId) {
    return findAuditEvent(auditId).orElse(null); //TODO: Don't return null.
  }

  private Optional<AuditEvent> findAuditEvent(final String auditId) {
    List<AuditEvent> eventList = getAuditEventsFromLog(auditId);

    return eventList.stream().filter(e -> e.getAuditId().equals(auditId)).findFirst();
  }

  @Override
  public boolean verifyEvent(AuditEvent auditEvent) {
    return verificationService.verifyEvent(auditEvent);
  }

  @Override
  public boolean completeVerification() {
    return verificationService.completeVerification();
  }

  private List<AuditEvent> getAuditEventsFromLog(String auditId) {
    ObjectName auditLogObjectName = getAuditLogObjectName(auditId);

    StoreObject storedDocumentAuditLog = objectStore.get(auditLogObjectName);

    List<AuditEvent> eventList = new ArrayList<>();

    if (null != storedDocumentAuditLog) {
      try {
        eventList = objectMapper.readValue(storedDocumentAuditLog.getInputStream(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, AuditEvent.class));
      } catch (IOException e) {
        LOGGER.error("Failed to deserialize document auditLog @{}\n@{}", auditLogObjectName, e);
        throw new DocumentServiceException("Failed to retrieve document auditLog");
      }
    }
    return eventList;
  }

  private ObjectName createAuditLogObjectName(final String documentId) {
    return ObjectName.of("document", documentId).append("auditLog.json");
  }

  private ObjectName getAuditLogObjectName(String auditId) {
    return createAuditLogObjectName(auditIdGenerationStrategy.getDocumentId(auditId));
  }
}