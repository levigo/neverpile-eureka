package com.neverpile.eureka.plugin.audit.rest;


import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.security.Principal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;
import com.neverpile.eureka.plugin.audit.verification.VerificationService;
import com.neverpile.eureka.rest.api.document.content.ContentElementResource;
import com.neverpile.eureka.rest.api.exception.NotFoundException;
import com.neverpile.urlcrypto.PreSignedUrlEnabled;

import io.micrometer.core.annotation.Timed;

@RestController
@RequestMapping(path = "/api/v1/documents/{documentId}/audit", produces = {
    MediaType.APPLICATION_JSON_VALUE
})
@ConditionalOnBean(AuditLogFacet.class)
public class AuditLogResource {
  private final ModelMapper modelMapper = new ModelMapper();

  @Autowired
  private DocumentService documentService;

  @Autowired
  private AuditLogService auditLogService;

  @Autowired
  private VerificationService verificationService;

  @Autowired
  ModelMapper documentMapper;

  @PreSignedUrlEnabled
  @GetMapping()
  @Timed(description = "get audit log", extraTags = {
      "operation", "retrieve", "target", "audit-log"
  }, value = "eureka.audit.log.get")
  public ResponseEntity<List<AuditEventDto>> getDocumentLog(@PathVariable("documentId") final String documentId) {

    List<AuditEventDto> auditLog = auditLogService.getEventLog(documentId).stream().map(audit -> {
      AuditEventDto auditDto = documentMapper.map(audit, AuditEventDto.class);
      auditDto.add(linkTo(ContentElementResource.class).slash(documentId) //
          .slash("audit").slash(audit.getAuditId()) //
          .withSelfRel());
      return auditDto;
    }).collect(Collectors.toList());

    Instant mostRecentEvent = auditLog.stream() //
        .map(AuditEventDto::getTimestamp) //
        .max(Comparator.naturalOrder()) //
        .orElse(Instant.now());

    return ResponseEntity.ok() //
        .lastModified(mostRecentEvent.toEpochMilli()) //
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) //
        .body(auditLog);
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "{auditId}")
  @Timed(description = "get audit event", extraTags = {
      "operation", "retrieve", "target", "audit-event"
  }, value = "eureka.audit.event.get")
  public ResponseEntity<AuditEventDto> getEvent(@PathVariable("auditId") final String auditId) {
    AuditEvent auditEvent = auditLogService.getEvent(auditId).orElseThrow(
        () -> new NotFoundException("AuditEventLog not found"));

    AuditEventDto res = documentMapper.map(auditEvent, AuditEventDto.class);

    return ResponseEntity.ok() //
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) //
        .body(res);
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "{auditId}/verify")
  @Timed(description = "verify audit event", extraTags = {
      "operation", "verify", "target", "audit-event"
  }, value = "eureka.audit.event.verify")
  public ResponseEntity<String> verifyEvent(@PathVariable("auditId") final String auditId) {
    AuditEvent auditEvent = auditLogService.getEvent(auditId).orElseThrow(
        () -> new NotFoundException("AuditEvent not found"));
    boolean res = verificationService.verifyEvent(auditEvent);
    return ResponseEntity.ok() //
        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE) //
        .body("Verificaton result: " + (res ? "OK" : ("Error" + "\n" + auditEvent.toString())));
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "verify")
  @Timed(description = "verify document audit events", extraTags = {
      "operation", "verify", "target", "audit-event"
  }, value = "eureka.audit.document.events.verify")
  public ResponseEntity<String> verifyDocumentLog(@PathVariable("documentId") final String documentId) {
    List<AuditEvent> auditLog = auditLogService.getEventLog(documentId);
    if (null == auditLog) {
      throw new NotFoundException("AuditEventLog not found");
    }
    boolean res = auditLog.size() > 0;
    AuditEvent errorEvent = null;
    for (AuditEvent auditEvent : auditLog) {
      res = verificationService.verifyEvent(auditEvent);
      if (!res) {
        errorEvent = auditEvent;
        break;
      }
    }
    return ResponseEntity.ok() //
        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE) //
        .body("Verificaton result: " + (res ? "OK" : ("Error" + "\n" + errorEvent.toString())));
  }

  @PostMapping
  @Transactional
  @Timed(description = "add audit event", extraTags = {
      "operation", "add", "target", "audit-event"
  }, value = "eureka.audit.add")
  public AuditEventDto create(final Principal principal, @PathVariable("documentId") final String documentId,
      @RequestBody @Valid @NotNull final AuditEventDto event) throws Exception {
    documentService.getDocument(documentId).orElseThrow(() -> new NotFoundException("Document not found"));

    event.setUserID(principal.getName());
    auditLogService.logEvent(modelMapper.map(event, AuditEvent.class));

    return event;
  }
}
