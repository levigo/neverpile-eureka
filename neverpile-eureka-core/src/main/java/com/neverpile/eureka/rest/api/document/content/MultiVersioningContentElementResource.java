package com.neverpile.eureka.rest.api.document.content;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Import;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neverpile.eureka.api.ContentElementService;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.exception.NotAcceptableException;
import com.neverpile.eureka.rest.api.exception.NotFoundException;
import com.neverpile.urlcrypto.PreSignedUrlEnabled;

import io.micrometer.core.annotation.Timed;

@RestController
@RequestMapping(path = "/api/v1/documents", produces = {
    MediaType.APPLICATION_JSON_VALUE
})
@Import(ContentElementResourceConfiguration.class)
@ConditionalOnBean(MultiVersioningDocumentService.class)
@Transactional
public class MultiVersioningContentElementResource extends ContentElementResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(MultiVersioningContentElementResource.class);

  @Autowired
  private MultiVersioningDocumentService documentService;

  @Autowired
  private ContentElementService contentElementService;

  @PreSignedUrlEnabled
  @GetMapping(value = "{documentID}/history/{versionTimestamp}/content/{element}")
  @Timed(description = "get content element", extraTags = {
      "operation", "retrieve", "target", "content"
  }, value = "eureka.content.get")
  public ResponseEntity<?> getById(@PathVariable("documentID") final String documentId,
      @PathVariable("versionTimestamp") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant versionTimestamp,
      @PathVariable("element") final String contentId) {
    // preconditions
    documentResource.validateDocumentId(documentId);
    assertContentExists(documentId, contentId);

    // fetch document and content elements
    Document document = documentService.getDocumentVersion(documentId, versionTimestamp) //
        .orElseThrow(() -> new NotFoundException("Document not found"));

    ContentElement contentElement = document.getContentElements().stream().filter(
        e -> e.getId().equals(contentId)).findFirst().orElseThrow(
            () -> new NotFoundException("Content element not found"));

    return returnSingleContentElement(document, contentElement);
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "{documentID}/history/{versionTimestamp}/content")
  @Timed(description = "get content element", extraTags = {
      "operation", "retrieve", "target", "content"
  }, value = "eureka.content.get")
  public ResponseEntity<?> query(@PathVariable("documentID") final String documentId,
      @PathVariable("versionTimestamp") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant versionTimestamp,
      @RequestParam(name = "role", required = false) final List<String> roles,
      @RequestParam(name = "return", required = false, defaultValue = "first") final Return ret) {
    // preconditions
    documentResource.validateDocumentId(documentId);

    // fetch document and content elements
    Document document = documentService.getDocument(documentId) //
        .orElseThrow(() -> new NotFoundException("Document not found"));

    Stream<ContentElement> elements = document.getContentElements().stream();

    // filter by roles
    if (null != roles)
      elements = elements.filter(ce -> roles.contains(ce.getRole()));

    List<ContentElement> matches = elements.collect(Collectors.toList());

    // return mode
    switch (ret){
      case only :
        if (matches.size() > 1)
          throw new NotAcceptableException("More than one content element matches the query");
        // fall-through

      case first :
        if (matches.isEmpty())
          throw new NotFoundException("No matching content element");

        return returnSingleContentElement(document, matches.get(0));

      case all :
        return returnMultipleElementsAsMultipart(document, matches);

      default :
        throw new NotAcceptableException("Unrecognized return mode");
    }
  }
}
