package com.neverpile.eureka.plugin.audit.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.security.Principal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.neverpile.eureka.rest.api.document.content.ContentElementResource;
import com.neverpile.eureka.rest.api.exception.NotFoundException;
import com.neverpile.urlcrypto.PreSignedUrlEnabled;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(path = "/api/v1/documents/{documentId}/audit", produces = {
    MediaType.APPLICATION_JSON_VALUE
})
@ConditionalOnBean(AuditLogFacet.class)
@OpenAPIDefinition(tags = @Tag(name = "Audit"))
public class AuditLogResource {
  private final ModelMapper modelMapper = new ModelMapper();

  @Autowired
  private DocumentService documentService;

  @Autowired
  private AuditLogService auditLogService;

  @Autowired
  @Qualifier("document")
  ModelMapper documentMapper;

  @PreSignedUrlEnabled
  @GetMapping()
  @Operation(summary = "Fetches a document's audit log")
  @ApiResponse(responseCode = "200", description = "Audit log found")
  @ApiResponse(responseCode = "404", description = "Document not found")
  @Timed(description = "get audit log", extraTags = {
      "operation", "retrieve", "target", "audit-log"
  }, value = "eureka.audit.log.get")
  public ResponseEntity<List<AuditEventDto>> get(
      @Parameter(description = "The ID of the document") @PathVariable("documentId") final String documentId) {

    List<AuditEventDto> auditLog = auditLogService.getEventLog(documentId).stream().map(audit -> {
      AuditEventDto auditDto = documentMapper.map(audit, AuditEventDto.class);
      auditDto.add(linkTo(ContentElementResource.class).slash(documentId) //
          .slash("audit").slash(audit.getAuditId()) //
          .withSelfRel());
      return auditDto;
    }).collect(Collectors.toList());

    Date mostRecentEvent = auditLog.stream() //
        .map(AuditEventDto::getTimestamp) //
        .max(Comparator.naturalOrder()) //
        .orElse(new Date());

    return ResponseEntity.ok() //
        .lastModified(mostRecentEvent.getTime()) //
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) //
        .body(auditLog);
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "{auditId}")
  @Operation(summary = "Fetches a single audit event")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Audit event found"),
      @ApiResponse(responseCode = "404", description = "AuditEvent not found")
  })
  @Timed(description = "get audit event", extraTags = {
      "operation", "retrieve", "target", "audit-event"
  }, value = "eureka.audit.event.get")
  public ResponseEntity<AuditEventDto> get(
      @Parameter(description = "The ID of the document") @PathVariable("documentId") final String documentId,
      @Parameter(description = "The ID of the audit event to be fetched") @PathVariable("auditId") final String auditId) {
    List<AuditEvent> auditLog = auditLogService.getEventLog(documentId);
    if (null == auditLog)
      throw new NotFoundException("AuditEventLog not found");

    for (AuditEvent auditEvent : auditLog) {
      if (auditEvent.getAuditId().equals(auditId)) {
        AuditEventDto res = documentMapper.map(auditEvent, AuditEventDto.class);
        res.add(linkTo(ContentElementResource.class).slash(documentId) //
            .slash("audit").slash(auditId).withSelfRel());

        return ResponseEntity.ok() //
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) //
            .body(res);
      }
    }

    throw new NotFoundException("Audit event not found");
  }

  @PostMapping
  @Operation(summary = "Appends an event to a document's audit log")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Event logged"),
      @ApiResponse(responseCode = "404", description = "Document not found"),
      @ApiResponse(responseCode = "400", description = "Invalid input, object invalid")
  })
  @Transactional
  @Timed(description = "add audit event", extraTags = {
      "operation", "add", "target", "audit-event"
  }, value = "eureka.audit.add")
  public AuditEventDto create(final Principal principal,
      @Parameter(description = "The ID of the document for which the event shall be logged") @PathVariable("documentId") final String documentId,
      @RequestBody @Valid @NotNull @NotBlank final AuditEventDto event) throws Exception {
    documentService.getDocument(documentId).orElseThrow(() -> new NotFoundException("Document not found"));

    event.setUserID(principal.getName());
    auditLogService.logEvent(documentId, modelMapper.map(event, AuditEvent.class));

    return event;
  }
}
