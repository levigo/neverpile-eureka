package com.neverpile.eureka.plugin.audit.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.api.DocumentService.DocumentServiceException;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.ObjectStoreService.ObjectStoreException;
import com.neverpile.eureka.api.ObjectStoreService.StoreObject;
import com.neverpile.eureka.impl.documentservice.DefaultDocumentService;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.service.AuditIdGenerationStrategy;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;
import com.neverpile.eureka.plugin.audit.verification.VerificationService;

public class SimpleAuditLogService implements AuditLogService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDocumentService.class);

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
    Optional<List<AuditEvent>> eventList = collectAuditLog(documentId);
    return eventList.orElseGet(ArrayList::new);
  }

  private Optional<List<AuditEvent>> collectAuditLog(final String documentId) {
    ObjectName auditLogObjectName = createDocumentAuditLogName(documentId);

    StoreObject storedDocumentAuditLog = objectStore.get(auditLogObjectName);

    if (null == storedDocumentAuditLog) {
      return Optional.empty();
    }

    // Pointer value is String array for ObjectName of auditEventBlock.
    List<String[]> eventPointerList;
    try {
      eventPointerList = objectMapper.readValue(storedDocumentAuditLog.getInputStream(),
          objectMapper.getTypeFactory().constructCollectionType(List.class, String[].class));
    } catch (IOException e) {
      LOGGER.error("Failed to deserialize document auditLog @{}\n@{}", auditLogObjectName, e);
      throw new DocumentServiceException("Failed to retrieve document auditLog");
    }

    if (null == eventPointerList) {
      return Optional.empty();
    }

    List<AuditEvent> resultList = new ArrayList<>();
    for (String[] pointer : eventPointerList) {
      List<AuditEvent> eventList = getAuditEventsFromBlock(ObjectName.of(pointer));

      resultList.addAll(
          eventList.stream().filter(e -> e.getDocumentId().equals(documentId)).collect(Collectors.toList()));
    }
    return Optional.of(resultList);
  }

  @Override
  public void logEvent(final AuditEvent event) {
    saveToAuditBlock(event);
    saveToDocumentLog(event);
    verificationService.processEvent(event);
  }

  private void saveToAuditBlock(AuditEvent event) {
    ObjectName auditBlockObjectName = createGlobalAuditBlockName(event.getAuditId());

    StoreObject storedDocumentAuditBlock = objectStore.get(auditBlockObjectName);

    List<AuditEvent> eventList = new ArrayList<>();
    String version = ObjectStoreService.NEW_VERSION;

    if (null != storedDocumentAuditBlock) {
      version = storedDocumentAuditBlock.getVersion();
      try {
        eventList = objectMapper.readValue(storedDocumentAuditBlock.getInputStream(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, AuditEvent.class));
      } catch (IOException e) {
        LOGGER.error("Failed to deserialize document auditLog @{}\n@{}", auditBlockObjectName, e);
        throw new DocumentServiceException("Failed to retrieve document auditLog");
      }
    }

    eventList.add(event);

    try {
      // FIXME: replace BAISO/BAOS with buffer manager for efficiency
      ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
      objectMapper.writeValue(baos, eventList);

      try {
        objectStore.put(auditBlockObjectName, version, new ByteArrayInputStream(baos.toByteArray()), baos.size());
      } catch (ObjectStoreException e) {
        LOGGER.error("Failed to store global auditBlock @{}\n@{}", auditBlockObjectName, e);
        throw new DocumentServiceException("Failed to store global auditBlock");
      }
    } catch (IOException e) {
      LOGGER.error("Failed to serialize global auditBlock", e);
      throw new DocumentServiceException("Failed to serialize global auditBlock");
    }
  }

  private void saveToDocumentLog(AuditEvent event) {
    ObjectName auditLogObjectName = createDocumentAuditLogName(event.getDocumentId());
    ObjectName eventPointerName = createGlobalAuditBlockName(event.getAuditId());

    StoreObject storedDocumentAuditBlock = objectStore.get(auditLogObjectName);

    List<String[]> eventPointerList = new ArrayList<>();
    String version = ObjectStoreService.NEW_VERSION;

    if (null != storedDocumentAuditBlock) {
      version = storedDocumentAuditBlock.getVersion();
      try {
        eventPointerList = objectMapper.readValue(storedDocumentAuditBlock.getInputStream(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, String[].class));
      } catch (IOException e) {
        LOGGER.error("Failed to deserialize document auditLog @{}\n@{}", auditLogObjectName, e);
        throw new DocumentServiceException("Failed to retrieve document auditLog");
      }
    }
    // If the current block is already in the Pointer list of the document nt, don't add duplicates.
    if (eventPointerList.stream().anyMatch(e -> ObjectName.of(e).compareTo(eventPointerName) == 0)) {
      return;
    }
    eventPointerList.add(eventPointerName.to());
    try {
      // FIXME: replace BAISO/BAOS with buffer manager for efficiency
      ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
      objectMapper.writeValue(baos, eventPointerList);

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
    return findAuditEvent(auditId).orElse(null);
  }

  private Optional<AuditEvent> findAuditEvent(final String auditId) {
    ObjectName auditBlockObjectName = createGlobalAuditBlockName(auditId);

    List<AuditEvent> eventList = getAuditEventsFromBlock(auditBlockObjectName);

    return eventList.stream().filter(e -> e.getAuditId().equals(auditId)).findFirst();
  }

  @Override
  public boolean verifyEvent(AuditEvent auditEvent) {
    return verificationService.verifyEvent(auditEvent);
  }

  @Override
  public boolean completeVerification() {
    return false;
  }

  private List<AuditEvent> getAuditEventsFromBlock(ObjectName auditBlockObjectName) {
    StoreObject storedDocumentAuditBlock = objectStore.get(auditBlockObjectName);

    List<AuditEvent> eventList = new ArrayList<>();

    if (null != storedDocumentAuditBlock) {
      try {
        eventList = objectMapper.readValue(storedDocumentAuditBlock.getInputStream(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, AuditEvent.class));
      } catch (IOException e) {
        LOGGER.error("Failed to deserialize document auditLog @{}\n@{}", auditBlockObjectName, e);
        throw new DocumentServiceException("Failed to retrieve document auditLog");
      }
    }
    return eventList;
  }

  private ObjectName createDocumentAuditLogName(final String documentId) {
    return ObjectName.of("document", documentId).append("auditLog.json");
  }

  private ObjectName createGlobalAuditBlockName(final String auditId) {
    return ObjectName.of(auditIdGenerationStrategy.getBlockId(auditId)).append("auditBlock.json");
  }
}
