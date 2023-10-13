package com.neverpile.eureka.plugin.metadata.rest;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
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

@RestController
@Controller
@RequestMapping(path = "/api/v1/documents/{documentID}/metadata", produces = {
    MediaType.APPLICATION_JSON_VALUE
})
@Transactional
public class MetadataResource {
  @Autowired
  private DocumentService documentService;

  @Autowired
  MetadataFacet facet;

  @Autowired
  ModelMapper documentMapper;

  @PreSignedUrlEnabled
  @GetMapping
  @Timed(description = "get document metadata", extraTags = {
      "operation", "retrieve", "target", "metadata"
  }, value = "eureka.metadata.get")
  public MetadataDto get(@PathVariable("documentID") final String documentId) {
    return facet.retrieve(fetchDocument(documentId)).orElseGet(MetadataDto::new);
  }

  private Document fetchDocument(final String documentId) {
    return documentService.getDocument(documentId).orElseThrow(() -> new NotFoundException("Document not found"));
  }

  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Timed(description = "update document metadata", extraTags = {
      "operation", "update", "target", "metadata"
  }, value = "eureka.metadata.update")
  public MetadataDto update(@PathVariable("documentID") final String documentId,
      @RequestBody final MetadataDto requestDto) {
    return facet.update(fetchDocument(documentId), requestDto);
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "{name}")
  @Timed(description = "get document metadata element", extraTags = {
      "operation", "retrieve", "target", "metadata-element"
  }, value = "eureka.metadata.element.get")
  public MetadataElementDto get(@PathVariable("documentID") final String documentId,
      @PathVariable("name") final String name) {
    MetadataElementDto elementDto = facet.retrieve(fetchDocument(documentId)).orElseGet(
        MetadataDto::new).getElements().get(name);

    if (null == elementDto)
      throw new NotFoundException("Element not found");

    return elementDto;
  }

  @PutMapping(value = "{name}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Timed(description = "update document metadata element", extraTags = {
      "operation", "update", "target", "metadata-element"
  }, value = "eureka.metadata.element.update")
  public MetadataElementDto update(@PathVariable("documentID") final String documentId,
      @PathVariable("name") final String name, @RequestBody final MetadataElementDto requestDto) {
    return facet.update(fetchDocument(documentId), name, requestDto);
  }

  @DeleteMapping(value = "{name}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Timed(description = "delete document metadata element", extraTags = {
      "operation", "delete", "target", "metadata-element"
  }, value = "eureka.metadata.element.delete")
  public void delete(@PathVariable("documentID") final String documentId, @PathVariable("name") final String name) {
    facet.delete(fetchDocument(documentId), name);
  }
}
