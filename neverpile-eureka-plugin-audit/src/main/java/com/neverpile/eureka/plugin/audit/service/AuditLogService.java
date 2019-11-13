package com.neverpile.eureka.plugin.audit.service;

import java.util.List;

public interface AuditLogService {
  List<AuditEvent> getEventLog(String documentId);

  void logEvent(String documentId, AuditEvent event);
}
