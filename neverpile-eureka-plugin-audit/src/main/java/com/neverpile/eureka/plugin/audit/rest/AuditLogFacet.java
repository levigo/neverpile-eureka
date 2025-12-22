package com.neverpile.eureka.plugin.audit.rest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.service.AuditEvent.Type;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;
import com.neverpile.eureka.plugin.audit.service.TimeBasedAuditIdGenerationStrategy;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.DocumentFacet;
import com.neverpile.eureka.rest.api.hateoas.Links;

@Component
@ConditionalOnProperty(name = "neverpile.facet.audit.enabled", matchIfMissing = true)
public class AuditLogFacet implements DocumentFacet<List<AuditEventDto>> {

  private final ModelMapper modelMapper = new ModelMapper();

  @Autowired
  private AuditLogService auditLogService;

  @Autowired
  private TimeBasedAuditIdGenerationStrategy idGenerationStrategy;

  @Override
  public String getName() {
    return "audit";
  }

  @Override
  public boolean includeByDefault() {
    return false; // the audit log is expensive and rarely examined
  }

  @Override
  public JavaType getValueType(final TypeFactory f) {
    return f.constructCollectionLikeType(List.class, AuditEventDto.class);
  }

  @Override
  public void onRetrieve(final Document document, final DocumentDto dto) {
    attachAuditLog(document, dto);

    dto.add(Links.facet(document, this));
  }

  @Override
  public void beforeCreate(final Document newDocument, final DocumentDto requestDto) {
    auditLogService.logEvent(createAuditEvent(Type.CREATE, newDocument.getDocumentId()));
  }

  @Override
  public void beforeUpdate(final Document currentDocument, final Document updatedDocument,
      final DocumentDto updateDto) {
    auditLogService.logEvent(createAuditEvent(Type.UPDATE, currentDocument.getDocumentId()));
  }

  @Override
  public void onDelete(final Document currentDocument) {
    auditLogService.logEvent(createAuditEvent(Type.DELETE, currentDocument.getDocumentId()));
  }

  private AuditEvent createAuditEvent(final Type type, String documentId) {
    AuditEvent eventDto = new AuditEvent();
    Instant timestamp = Instant.now();
    eventDto.setTimestamp(timestamp);
    eventDto.setType(type);
    eventDto.setAuditId(idGenerationStrategy.createAuditId(timestamp, documentId));
    eventDto.setDocumentId(documentId);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth.isAuthenticated()) {
      eventDto.setUserID(auth.getName());
    } else {
      eventDto.setUserID("<anonymous>");
    }

    return eventDto;
  }

  private void attachAuditLog(final Document document, final DocumentDto dto) {
    dto.setFacet(getName(), auditLogService.getEventLog(document.getDocumentId()).stream().map(audit -> {
          AuditEventDto auditDto = modelMapper.map(audit, AuditEventDto.class);

          auditDto.add(Links.facet(document, this, audit.getAuditId()));

          return auditDto;
        }).collect(Collectors.toList()) //
    );
  }

  @Override
  public String toString() {
    return "DocumentFacet{" + "name=" + getName() + '}';
  }
}

