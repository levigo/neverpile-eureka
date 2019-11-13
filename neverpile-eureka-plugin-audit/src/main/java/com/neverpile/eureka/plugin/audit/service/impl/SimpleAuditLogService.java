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
import com.neverpile.eureka.impl.documentservice.DefaultDocumentService;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;

public class SimpleAuditLogService implements AuditLogService {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDocumentService.class);

	@Autowired
	private ObjectStoreService objectStore;

	@Autowired
	private ObjectMapper objectMapper;

	private Optional<List<AuditEvent>> findAuditLog(final String documentId) {
		ObjectName auditLogObjectName = createAuditLogObjectName(documentId);

		StoreObject storedDocumentAuditLog = objectStore.get(auditLogObjectName);

		if (null == storedDocumentAuditLog)
			return Optional.empty();

		try {
			return Optional.ofNullable(objectMapper.readValue(storedDocumentAuditLog.getInputStream(),
					objectMapper.getTypeFactory().constructCollectionType(List.class, AuditEvent.class)));
		} catch (IOException e) {
			LOGGER.error("Failed to deserialize document auditLog @{}", auditLogObjectName, e);
			throw new DocumentServiceException("Failed to retrieve document auditLog");
		}
	}

	private ObjectName createAuditLogObjectName(final String documentId) {
		return ObjectName.of("document", documentId).append("auditLog.json");
	}

	@Override
	public List<AuditEvent> getEventLog(final String documentId) {
		Optional<List<AuditEvent>> eventList = findAuditLog(documentId);
		return eventList.orElseGet(ArrayList::new);
	}

	@Override
	public void logEvent(final String documentId, final AuditEvent event) {

		ObjectName auditLogObjectName = createAuditLogObjectName(documentId);

		StoreObject storedDocumentAuditLog = objectStore.get(auditLogObjectName);

		List<AuditEvent> eventList = new ArrayList<>();
		String version = ObjectStoreService.NEW_VERSION;

		if (null != storedDocumentAuditLog) {
			version = storedDocumentAuditLog.getVersion();
			try {
				eventList = objectMapper.readValue(storedDocumentAuditLog.getInputStream(),
						objectMapper.getTypeFactory().constructCollectionType(List.class, AuditEvent.class));
			} catch (IOException e) {
				LOGGER.error("Failed to deserialize document auditLog @{}", auditLogObjectName, e);
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
				LOGGER.error("Failed to store document auditLog @{}", auditLogObjectName, e);
				throw new DocumentServiceException("Failed to store document auditLog");
			}
		} catch (IOException e) {
			LOGGER.error("Failed to serialize document auditLog", e);
			throw new DocumentServiceException("Failed to serialize document auditLog");
		}
	}
}
