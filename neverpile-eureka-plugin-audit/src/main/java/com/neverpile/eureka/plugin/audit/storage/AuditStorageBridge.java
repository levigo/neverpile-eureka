package com.neverpile.eureka.plugin.audit.storage;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;

public interface AuditStorageBridge {

  public void putAuditEvent(AuditEvent auditEvent);

  public Optional<AuditEvent> getAuditEvent(String auditId);

  public List<AuditEvent> getDocumentAuditLog(String documentId);

  public void putVerificationElement(ObjectName key, InputStream verificationElement, int length);

  public Optional<InputStream> getVerificationElement(ObjectName key);

  public void updateHeadVerificationElement(InputStream verificationElement, int length);

  public Optional<InputStream> getHeadVerificationElement();

}
