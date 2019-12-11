package com.neverpile.eureka.plugin.metadata.rest;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.exception.NotFoundException;
import com.neverpile.urlcrypto.PreSignedUrlEnabled;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(path = "/api/v1/documents/{documentID}/metadata", produces = {
    MediaType.APPLICATION_JSON_VALUE
})
@OpenAPIDefinition(tags = @Tag(name = "Metadata"))
public class MetadataResource {
  @Autowired
  private DocumentService documentService;

  @Autowired
  MetadataFacet facet;

  @Autowired
  @Qualifier("document")
  ModelMapper documentMapper;

  @PreSignedUrlEnabled
  @GetMapping
  @Operation(description = "Fetches a document's metadata")
  @ApiResponse(responseCode = "200", description = "Document metadata found")
  @ApiResponse(responseCode = "404", description = "Document not found")
  @Timed(description = "get document metadata", extraTags = {
      "operation", "retrieve", "target", "metadata"
  }, value = "eureka.metadata.get")
  public MetadataDto get(
      @Parameter(description = "The ID of the document for which metadata shall be fetched") @PathVariable("documentID") final String documentId) {
    return facet.retrieve(fetchDocument(documentId)).orElseGet(MetadataDto::new);
  }

  private Document fetchDocument(final String documentId) {
    return documentService.getDocument(documentId).orElseThrow(() -> new NotFoundException("Document not found"));
  }

  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Updates a document's metadata")
  @ApiResponse(responseCode = "202", description = "Metadata updated")
  @ApiResponse(responseCode = "404", description = "Document not found")
  @Transactional
  @Timed(description = "update document metadata", extraTags = {
      "operation", "update", "target", "metadata"
  }, value = "eureka.metadata.update")
  public MetadataDto update(
      @Parameter(description = "The ID of the document to be updated") @PathVariable("documentID") final String documentId,
      @Parameter @RequestBody final MetadataDto requestDto) {
    return facet.update(fetchDocument(documentId), requestDto);
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "{name}")
  @Operation(summary = "Fetches a document metadata element by ID and element name")
  @ApiResponse(responseCode = "200", description = "Metadata element found")
  @ApiResponse(responseCode = "404", description = "Document or element not found")
  @Timed(description = "get document metadata element", extraTags = {
      "operation", "retrieve", "target", "metadata-element"
  }, value = "eureka.metadata.element.get")
  public MetadataElementDto get(
      @Parameter(description = "The ID of the document's metadata to be fetched") @PathVariable("documentID") final String documentId,
      @Parameter(description = "The name of the metadata element to be fetched") @PathVariable("name") final String name) {
    MetadataElementDto elementDto = facet.retrieve(fetchDocument(documentId)).orElseGet(
        MetadataDto::new).getElements().get(name);

    if (null == elementDto)
      throw new NotFoundException("Element not found");

    return elementDto;
  }

  @PutMapping(value = "{name}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  @Operation(summary = "Create or update a single metatadata element of a document")
  @ApiResponse(responseCode = "202", description = "Metadata updated")
  @ApiResponse(responseCode = "404", description = "Document not found")
  @Timed(description = "update document metadata element", extraTags = {
      "operation", "update", "target", "metadata-element"
  }, value = "eureka.metadata.element.update")
  public MetadataElementDto update(
      @Parameter(description = "ID of the document to be updated") @PathVariable("documentID") final String documentId,
      @Parameter(description = "Name of the metadata element to be created/updated") @PathVariable("name") final String name,
      @Parameter @RequestBody final MetadataElementDto requestDto) {
    return facet.update(fetchDocument(documentId), name, requestDto);
  }

  @DeleteMapping(value = "{name}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Delete a single metatadata element of a document")
  @ApiResponse(responseCode = "204", description = "Metadata element deleted")
  @ApiResponse(responseCode = "404", description = "Document or element not found")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  @Timed(description = "delete document metadata element", extraTags = {
      "operation", "delete", "target", "metadata-element"
  }, value = "eureka.metadata.element.delete")
  public void delete(
      @Parameter(description = "ID of the document to be updated") @PathVariable("documentID") final String documentId,
      @Parameter(description = "The name of the metadata element to be deleted") @PathVariable("name") final String name) {
    facet.delete(fetchDocument(documentId), name);
  }
}
