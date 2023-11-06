package com.neverpile.eureka.rest.api.document.content;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Import;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neverpile.eureka.api.ContentElementService;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentResource;
import com.neverpile.eureka.rest.api.document.content.ContentElementResource.Return;
import com.neverpile.eureka.rest.api.exception.NotFoundException;
import com.neverpile.urlcrypto.PreSignedUrlEnabled;

import io.micrometer.core.annotation.Timed;

@RestController
@RequestMapping(path = "/api/v1/documents", produces = MediaType.APPLICATION_JSON_VALUE)
@Import(ContentElementResourceConfiguration.class)
@ConditionalOnBean(MultiVersioningDocumentService.class)
@Transactional
public class MultiVersioningContentElementResource {
  private final MultiVersioningDocumentService multiVersioningDocumentService;

  private final DocumentResource documentResource;
  
  private final ContentElementService contentElementService;
  
  @Autowired
  public MultiVersioningContentElementResource(final MultiVersioningDocumentService multiVersioningDocumentService, final DocumentResource documentResource, final ContentElementService contentElementService) {
    this.multiVersioningDocumentService = multiVersioningDocumentService;
    this.documentResource = documentResource;
    this.contentElementService = contentElementService;
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "{documentID}/history/{versionTimestamp}/content", produces = MediaType.ALL_VALUE)
  @Timed(description = "query content element of particular version", extraTags = {
      "operation", "query", "target", "content"
  }, value = "eureka.content.get")
  public ResponseEntity<?> query(@PathVariable("documentID") final String documentId,
      @PathVariable("versionTimestamp") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant versionTimestamp,
      @RequestParam(name = "role", required = false) final List<String> roles,
      @RequestParam(name = "return", required = false, defaultValue = "first") final Return ret,
      @RequestHeader(name = "Accept") final List<String> acceptHeader) {
    // preconditions
    documentResource.validateDocumentId(documentId);

    // fetch document and content elements
    Document document = multiVersioningDocumentService.getDocumentVersion(documentId, versionTimestamp) //
        .orElseThrow(() -> new NotFoundException("Document not found"));

    return ContentElementResource.returnMatches(ret, document, ContentElementResource.applyFilters(roles, acceptHeader, document), contentElementService);
  }
}
