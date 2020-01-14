package com.neverpile.eureka.plugin.audit.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.api.NeverpileException;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.service.AuditIdGenerationStrategy;

public class AuditObjectStoreBridge implements AuditStorageBridge {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditObjectStoreBridge.class);

  @Autowired
  ObjectStoreService objectStoreService;

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  AuditIdGenerationStrategy auditIdGenerationStrategy;

  private final ObjectName currentVerificationName = ObjectName.of("eureka", "verification", "currentHead");

  private String currentVerificationVersion;

  @Override
  public void putAuditEvent(AuditEvent auditEvent) {

    ObjectName auditLogObjectName = getAuditObjectNameFromDocumentID(auditEvent.getDocumentId());

    ObjectStoreService.StoreObject storedDocumentAuditLog = objectStoreService.get(auditLogObjectName);

    List<AuditEvent> eventList = getAuditEventList(storedDocumentAuditLog);
    String version = getObjectVersion(storedDocumentAuditLog);

    eventList.add(auditEvent);

    putAuditEventList(eventList, auditLogObjectName, version);
  }

  @Override
  public Optional<AuditEvent> getAuditEvent(String auditId) {

    ObjectName auditLogObjectName = getAuditObjectNameFromAuditID(auditId);

    List<AuditEvent> eventList = getAuditEventList(objectStoreService.get(auditLogObjectName));

    return eventList.stream().filter(e -> e.getAuditId().equals(auditId)).findFirst();
  }

  @Override
  public List<AuditEvent> getDocumentAuditLog(String documentId) {

    ObjectName auditLogObjectName = getAuditObjectNameFromDocumentID(documentId);

    return getAuditEventList(objectStoreService.get(auditLogObjectName));
  }

  @Override
  public void putVerificationElement(ObjectName key, InputStream verificationElement, int length) {
    try {
      objectStoreService.put(key, ObjectStoreService.NEW_VERSION, verificationElement, length);
    } catch (ObjectStoreService.ObjectStoreException e) {
      LOGGER.error("Failed to store verification for auditLog @{}", key, e);
      throw new DocumentService.DocumentServiceException("Failed to store verification for auditLog");
    }
  }

  @Override
  public Optional<InputStream> getVerificationElement(ObjectName key) {
    ObjectStoreService.StoreObject so = objectStoreService.get(key);
    if (null != so) {
      return Optional.ofNullable(so.getInputStream());
    }
    return Optional.empty();
  }

  @Override
  public void updateHeadVerificationElement(InputStream verificationElement, int length) {
    try {
      Optional<InputStream> is = getHeadVerificationElement(); // To update currentVerificationVersion.
      is.ifPresent(i -> {
        try {
          i.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      objectStoreService.put(currentVerificationName, currentVerificationVersion, verificationElement, length);
    } catch (ObjectStoreService.ObjectStoreException e) {
      LOGGER.error("Failed to store verification Head for auditLog", e);
      throw new DocumentService.DocumentServiceException("Failed to store verification for auditLog");
    }
  }

  @Override
  public Optional<InputStream> getHeadVerificationElement() {
    ObjectStoreService.StoreObject so = objectStoreService.get(currentVerificationName);
    if (null != so) {
      currentVerificationVersion = so.getVersion();
      return Optional.ofNullable(so.getInputStream());
    }
    currentVerificationVersion = ObjectStoreService.NEW_VERSION;
    return Optional.empty();
  }

  private ObjectName getAuditObjectNameFromDocumentID(final String documentId) {
    return ObjectName.of("document", documentId).append("auditLog.json");
  }

  private ObjectName getAuditObjectNameFromAuditID(String auditId) {
    return getAuditObjectNameFromDocumentID(auditIdGenerationStrategy.getDocumentId(auditId));
  }

  private void putAuditEventList(List<AuditEvent> eventList, ObjectName auditLogObjectName, String version) {
    try {
      // FIXME: replace BAISO/BAOS with buffer manager for efficiency
      ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
      objectMapper.writeValue(baos, eventList);

      try {
        objectStoreService.put(auditLogObjectName, version, new ByteArrayInputStream(baos.toByteArray()), baos.size());
      } catch (ObjectStoreService.ObjectStoreException e) {
        LOGGER.error("Failed to store document auditLog @{}", auditLogObjectName, e);
        throw new NeverpileException("Failed to store document auditLog");
      }
    } catch (IOException e) {
      LOGGER.error("Failed to serialize document auditLog", e);
      throw new NeverpileException("Failed to serialize document auditLog");
    }
  }

  private List<AuditEvent> getAuditEventList(ObjectStoreService.StoreObject storedDocumentAuditLog) {
    ArrayList list = new ArrayList<>();
    if (null != storedDocumentAuditLog) {
      try {
        list = objectMapper.readValue(storedDocumentAuditLog.getInputStream(),
            objectMapper.getTypeFactory().constructCollectionType(List.class, AuditEvent.class));
      } catch (IOException e) {
        LOGGER.error("Failed to deserialize document auditLog", e);
        throw new NeverpileException("Failed to retrieve document auditLog");
      }
    }
    return list;
  }

  private String getObjectVersion(ObjectStoreService.StoreObject storedDocumentAuditLog) {
    String version = ObjectStoreService.NEW_VERSION;
    if (null != storedDocumentAuditLog) {
      version = storedDocumentAuditLog.getVersion();
    }
    return version;
  }
}
