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
import com.neverpile.eureka.rest.psu.PreSignedUrlEnabled;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@RestController
@RequestMapping(path = "/api/v1/documents/{documentID}/metadata", produces = {
    MediaType.APPLICATION_JSON_VALUE
})
@Api(tags = "Metadata", authorizations = {
    @Authorization(value = "oauth")
})
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
  @ApiOperation("Fetches a document's metadata")
  @ApiResponses({
      @ApiResponse(code = 200, message = "Document metadata found"),
      @ApiResponse(code = 404, message = "Document not found")
  })
  @Timed(description = "get document metadata", extraTags = {"operation", "retrieve", "target", "metadata"}, value="eureka.metadata.get")
  public MetadataDto get(
      @ApiParam(value = "The ID of the document for which metadata shall be fetched") @PathVariable("documentID") final String documentId) {
    return facet.retrieve(fetchDocument(documentId)).orElseGet(MetadataDto::new);
  }

  private Document fetchDocument(final String documentId) {
    return documentService.getDocument(documentId).orElseThrow(() -> new NotFoundException("Document not found"));
  }

  @PutMapping(consumes = {
      MediaType.APPLICATION_JSON_VALUE
  })
  @ApiOperation(value = "Updates a document's metadata")
  @ApiResponses({
      @ApiResponse(code = 202, message = "Metadata updated"), @ApiResponse(code = 404, message = "Document not found")
  })
  @Transactional
  @Timed(description = "update document metadata", extraTags = {"operation", "update", "target", "metadata"}, value="eureka.metadata.update")
  public MetadataDto update(
      @ApiParam(value = "The ID of the document to be updated") @PathVariable("documentID") final String documentId,
      @ApiParam @RequestBody final MetadataDto requestDto) {
    return facet.update(fetchDocument(documentId), requestDto);
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "{name}")
  @ApiOperation(value = "Fetches a document metadata element by ID and element name")
  @ApiResponses({
      @ApiResponse(code = 200, message = "Metadata element found"),
      @ApiResponse(code = 404, message = "Document or element not found")
  })
  @Timed(description = "get document metadata element", extraTags = {"operation", "retrieve", "target", "metadata-element"}, value="eureka.metadata.element.get")
  public MetadataElementDto get(
      @ApiParam(value = "The ID of the document's metadata to be fetched") @PathVariable("documentID") final String documentId,
      @ApiParam(value = "The name of the metadata element to be fetched") @PathVariable("name") final String name) {
    MetadataElementDto elementDto = facet.retrieve(fetchDocument(documentId)).orElseGet(MetadataDto::new).getElements().get(name);

    if (null == elementDto)
      throw new NotFoundException("Element not found");

    return elementDto;
  }

  @PutMapping(value = "{name}", consumes = {
      MediaType.APPLICATION_JSON_VALUE
  })
  @ApiOperation(value = "Create or update a single metatadata element of a document")
  @ApiResponses({
      @ApiResponse(code = 202, message = "metadata updated"), @ApiResponse(code = 404, message = "Document not found")
  })
  @Transactional
  @Timed(description = "update document metadata element", extraTags = {"operation", "update", "target", "metadata-element"}, value="eureka.metadata.element.update")
  public MetadataElementDto update(
      @ApiParam(value = "ID of the document to be updated") @PathVariable("documentID") final String documentId,
      @ApiParam(value = "Name of the metadata element to be created/updated") @PathVariable("name") final String name,
      @ApiParam @RequestBody final MetadataElementDto requestDto) {
    return facet.update(fetchDocument(documentId), name, requestDto);
  }

  @DeleteMapping(value = "{name}", consumes = {
      MediaType.APPLICATION_JSON_VALUE
  })
  @ApiOperation(value = "Delete a single metatadata element of a document")
  @ApiResponses({
      @ApiResponse(code = 204, message = "Metadata element deleted"),
      @ApiResponse(code = 404, message = "Document or element not found")
  })
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  @Timed(description = "delete document metadata element", extraTags = {"operation", "delete", "target", "metadata-element"}, value="eureka.metadata.element.delete")
  public void delete(
      @ApiParam(value = "ID of the document to be updated") @PathVariable("documentID") final String documentId,
      @ApiParam(value = "The name of the metadata element to be deleted") @PathVariable("name") final String name) {
    facet.delete(fetchDocument(documentId), name);
  }
}
