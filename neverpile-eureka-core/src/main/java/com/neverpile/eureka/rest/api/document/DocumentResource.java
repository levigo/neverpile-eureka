package com.neverpile.eureka.rest.api.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentFacet.ConstraintViolation;
import com.neverpile.eureka.rest.api.exception.AlreadyExistsException;
import com.neverpile.eureka.rest.api.exception.BadInputParameter;
import com.neverpile.eureka.rest.api.exception.NotFoundException;
import com.neverpile.eureka.rest.api.exception.ValidationError;
import com.neverpile.urlcrypto.PreSignedUrlEnabled;

import io.micrometer.core.annotation.Timed;

@RestController
@Controller
@RequestMapping(path = "/api/v1/documents", produces = {
    MediaType.APPLICATION_JSON_VALUE
})
@Transactional
public class DocumentResource {

  @Autowired
  private DocumentService documentService;

  @Autowired
  private DocumentIdGenerationStrategy idGenerationStrategy;

  @Autowired(required = false)
  private final List<DocumentFacet<?>> facets = new ArrayList<DocumentFacet<?>>();

  @Autowired
  ModelMapper documentMapper;

  @Autowired
  ObjectMapper mapper;

  // GET - Returns a specific document by ID
  @PreSignedUrlEnabled
  @GetMapping(value = "{documentID}")
  @Timed(description = "get document", extraTags = {
      "operation", "retrieve", "target", "document"
  }, value = "eureka.document.get")
  public DocumentDto get(@PathVariable("documentID") final String documentId,
      @RequestParam(name = "facets", required = false) final List<String> requestedFacets) {
    // @formatter:on
    Document document = getDocument(documentId);

    DocumentDto dto = new DocumentDto();

    activeFacets(requestedFacets, f -> f.onRetrieve(document, dto));

    return dto;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Timed(description = "create document", extraTags = {
      "operation", "create", "target", "document"
  }, value = "eureka.document.create")
  public DocumentDto create(@RequestBody final DocumentDto requestDto,
      @RequestParam(name = "facets", required = false) final List<String> requestedFacets)
      throws JsonMappingException, JsonProcessingException {

    validate(f -> f.validateCreate(requestDto));

    Document newDocument = documentMapper.map(requestDto, Document.class);
    if (checkDocumentExist(newDocument.getDocumentId())) {
      throw new AlreadyExistsException("DocumentId already exists");
    }

    allFacets(f -> f.beforeCreate(newDocument, requestDto));

    Document created = documentService.createDocument(newDocument);
    DocumentDto responseDto = new DocumentDto();

    // will populate everything, therefore...
    allFacets(f -> f.afterCreate(created, responseDto));

    // ...prune unwanted stuff before returning
    // FIXME: come up with a better solution.
    // Discussion so far:
    // https://levigo.de/stash/projects/CUBE/repos/neverpile-eureka/pull-requests/20/overview?commentId=19351
    pruneUnwantedFacetData(requestedFacets, responseDto);

    return responseDto;
  }

  // just some syntactic sugar
  private void allFacets(final Consumer<DocumentFacet<?>> facetConsumer) {
    facets.forEach(facetConsumer);
  }

  public void validate(final Function<DocumentFacet<?>, Set<ConstraintViolation>> validationFunction) {
    Set<ConstraintViolation> violations = facets.stream() //
        .map(f -> validationFunction.apply(f)) //
        .flatMap(r -> r.stream()) //
        .collect(Collectors.toSet());

    if (!violations.isEmpty())
      throw new ValidationError(violations);
  }

  private void pruneUnwantedFacetData(final List<String> requestedFacets, final DocumentDto responseDto) {
    responseDto.getFacets().keySet().removeAll(unwantedFacets(requestedFacets));
  }

  private void activeFacets(final List<String> requestedFacets, final Consumer<DocumentFacet<?>> facetConsumer) {
    (requestedFacets != null && !requestedFacets.isEmpty()
        ? facets.stream().filter(f -> requestedFacets.contains(f.getName()))
        : facets.stream().filter(DocumentFacet::includeByDefault)) //
            .forEach(facetConsumer);
  }

  private Collection<String> unwantedFacets(final List<String> requestedFacets) {
    return (requestedFacets != null && !requestedFacets.isEmpty()
        ? facets.stream().filter(f -> !requestedFacets.contains(f.getName()))
        : facets.stream().filter(f -> !f.includeByDefault())) //
            .map(f -> f.getName()).collect(Collectors.toSet());
  }

  @PutMapping(value = "{documentID}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Timed(description = "update document", extraTags = {
      "operation", "update", "target", "document"
  }, value = "eureka.document.update")
  public DocumentDto update(final HttpServletRequest request, @PathVariable("documentID") final String documentId,
      @RequestBody final DocumentDto requestDto,
      @RequestParam(name = "facets", required = false) final List<String> requestedFacets) {
    if (!checkDocumentExist(documentId)) {
      throw new NotFoundException("Document not found");
    }

    if (StringUtils.isEmpty(requestDto.getDocumentId())) {
      requestDto.setDocumentId(documentId);
    }
    return update(requestDto, getDocument(documentId), requestedFacets);
    // @formatter:off
  }

  public DocumentDto update(final DocumentDto requestDto, final Document updatedDocument, final List<String> requestedFacets) {
    Document currentDocument = getDocument(requestDto.getDocumentId());

    validate(f -> f.validateUpdate(currentDocument, requestDto));

    allFacets(f -> f.beforeUpdate(currentDocument, updatedDocument, requestDto));
    
    Document updated = documentService.update(updatedDocument) //
        .orElseThrow(() -> new NotFoundException("Document not found"));

    DocumentDto responseDto = new DocumentDto();

    // will populate everything, therefore...
    allFacets(f -> f.afterUpdate(updated, responseDto));

    // ...prune unwanted stuff before returning
    pruneUnwantedFacetData(requestedFacets, responseDto);

    return responseDto;
  }

  @DeleteMapping(value = "{documentID}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Timed(description = "delete document", extraTags = {"operation", "delete", "target", "document"}, value="eureka.document.delete")
  public void delete(
      @PathVariable("documentID") final String documentId) {
    Document document = getDocument(documentId);
    if (null == document) {
      throw new NotFoundException("Document not found");
    }

    allFacets(f -> f.validateDelete(document));

    allFacets(f -> f.onDelete(document));

    documentService.deleteDocument(documentId);
  }

  /**
   * The method check_BadInputParameter checks the documentId for a specific pattern. If the Id
   * does not match the required pattern, this method throws an API-Exception.
   *
   * @param documentId Id to check
   */
  public void validateDocumentId(final String documentId) {
    if (!idGenerationStrategy.validateDocumentId(documentId)) {
      throw new BadInputParameter("Invalid documentID supplied");
    }
  }

  /**
   * The get_Document method returns the document with the given id. If no document could be found,
   * this method throws an API-Exception.
   *
   * @param documentId Id of document to get
   * @return the document or throws an API-Exception
   */
  private Document getDocument(final String documentId) {
    return documentService.getDocument(documentId).orElseThrow(() -> new NotFoundException("Document not found"));
  }

  /**
   * The checkDocumentExist method checks if there is a document with the given id.
   *
   * @param documentId Id of document to check
   * @return false, if this is not the case
   */
  private boolean checkDocumentExist(final String documentId) {
    return documentService.documentExists(documentId);
  }

}
