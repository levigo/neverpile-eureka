package com.neverpile.eureka.rest.api.document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.exception.NotFoundException;
import com.neverpile.urlcrypto.PreSignedUrlEnabled;

import io.micrometer.core.annotation.Timed;

/**
 * Provide access to a document's history given that a {@link MultiVersioningDocumentService} is
 * available.
 */
@RestController
@RequestMapping(path = "/api/v1/documents/{documentID}/history", produces = {
    MediaType.APPLICATION_JSON_VALUE
})
@ConditionalOnBean(MultiVersioningDocumentService.class)
public class MultiVersioningDocumentResource {

  @Autowired
  private MultiVersioningDocumentService documentService;

  @Autowired(required = false)
  private final List<DocumentFacet<?>> facets = new ArrayList<DocumentFacet<?>>();

  @Autowired
  private ModelMapper documentMapper;

  // GET - Return the version history
  @PreSignedUrlEnabled
  @GetMapping
  @Timed(description = "get document history", extraTags = {
      "operation", "retrieve", "target", "history"
  }, value = "eureka.document.history.get")
  public List<Instant> get(@PathVariable("documentID") final String documentId) {
    List<Instant> versions = documentService.getVersions(documentId);

    // "convert" empty version list to not found
    if (versions.isEmpty())
      throw new NotFoundException("Document " + documentId + " not found");

    return versions;
  }

  // GET - Returns a specific document version by ID and time stamp
  @PreSignedUrlEnabled
  @GetMapping(value = "/{versionTimestamp}")
  @Timed(description = "get document", extraTags = {
      "operation", "retrieve", "target", "history"
  }, value = "eureka.document.history.get-version")
  public DocumentDto get(@PathVariable("documentID") final String documentId,
      @PathVariable("versionTimestamp") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant versionTimestamp,
      @RequestParam(name = "facets", required = false) final List<String> requestedFacets) {
    // @formatter:on
    Document document = documentService.getDocumentVersion(documentId, versionTimestamp).orElseThrow(
        () -> new NotFoundException("Document or version not found"));

    DocumentDto dto = documentMapper.map(document, DocumentDto.class);

    activeFacets(requestedFacets, f -> f.onRetrieve(document, dto));

    return dto;
  }

  private void activeFacets(final List<String> requestedFacets, final Consumer<DocumentFacet<?>> facetConsumer) {
    (requestedFacets != null && !requestedFacets.isEmpty()
        ? facets.stream().filter(f -> requestedFacets.contains(f.getName()))
        : facets.stream().filter(DocumentFacet::includeByDefault)) //
            .forEach(facetConsumer);
  }
}
