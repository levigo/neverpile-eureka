package com.neverpile.eureka.rest.api.document.content;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.InputStreamResource;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.api.ContentElementService;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.DocumentResource;
import com.neverpile.eureka.rest.api.document.content.AllRequestPartsMethodArgumentResolver.AllRequestParts;
import com.neverpile.eureka.rest.api.document.core.ModificationDateFacet;
import com.neverpile.eureka.rest.api.exception.ConflictException;
import com.neverpile.eureka.rest.api.exception.NotAcceptableException;
import com.neverpile.eureka.rest.api.exception.NotFoundException;
import com.neverpile.urlcrypto.PreSignedUrlEnabled;

import io.micrometer.core.annotation.Timed;

@RestController
@RequestMapping(path = "/api/v1/documents", produces = MediaType.APPLICATION_JSON_VALUE)
@Import(ContentElementResourceConfiguration.class)
@Transactional
public class ContentElementResource {
  private static final String VERSION_TIMESTAMP_HEADER = "X-NPE-Document-Version-Timestamp";

  public static final String DOCUMENT_FORM_ELEMENT_NAME = "__DOC";

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentElementResource.class);

  private static final Type CE_DTO_TYPE = new TypeToken<List<ContentElementDto>>() {
  }.getType();

  @Autowired
  private DocumentService documentService;

  @Autowired
  private ContentElementService contentElementService;

  @Autowired
  private ModelMapper documentMapper;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  protected DocumentResource documentResource;

  @Autowired
  private DocumentIdGenerationStrategy idGenerationStrategy;

  private MessageDigest messageDigest;

  @Value("${neverpile-eureka.message-digest-algorithm:SHA-256}")
  private String messageDigestAlgorithm;

  @Autowired
  private ModificationDateFacet mdFacet;

  @PostConstruct
  public void init() throws NoSuchAlgorithmException {
    messageDigest = MessageDigest.getInstance(messageDigestAlgorithm);
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "{documentID}/content/{element}", produces = MediaType.ALL_VALUE)
  @Timed(description = "get content element", extraTags = {
      "operation", "retrieve", "target", "content"
  }, value = "eureka.content.get")
  public ResponseEntity<?> getById(@PathVariable("documentID") final String documentId,
      @PathVariable("element") final String contentId) {
    // preconditions
    documentResource.validateDocumentId(documentId);
    assertContentExists(documentId, contentId);

    // fetch document and content elements
    Document document = documentService.getDocument(documentId) //
        .orElseThrow(() -> new NotFoundException("Document not found"));

    ContentElement contentElement = document.getContentElements().stream().filter(
        e -> e.getId().equals(contentId)).findFirst().orElseThrow(
            () -> new NotFoundException("Content element not found"));

    return returnSingleContentElement(document, contentElement, contentElementService);
  }

  public enum Return {
    /**
     * Return the only element matching the query. Fail if more than one element matches.
     */
    only,
    /**
     * Return the first element matching the query. Silently ignore other matches.
     */
    first,
    /**
     * Return all elements matching the query using a MIME Multipart body.
     */
    all
  }

  @PreSignedUrlEnabled
  @GetMapping(value = "{documentID}/content", produces = MediaType.ALL_VALUE)
  @Timed(description = "get content element", extraTags = {
      "operation", "retrieve", "target", "content"
  }, value = "eureka.content.get")
  public ResponseEntity<?> query(@PathVariable("documentID") final String documentId,
      @RequestParam(name = "role", required = false) final List<String> roles,
      @RequestParam(name = "return", required = false, defaultValue = "first") final Return ret,
      @RequestHeader(name = "Accept") final List<String> acceptHeader) {
    // preconditions
    documentResource.validateDocumentId(documentId);

    // fetch document and content elements
    Document document = documentService.getDocument(documentId) //
        .orElseThrow(() -> new NotFoundException("Document not found"));

    return returnMatches(ret, document, applyFilters(roles, acceptHeader, document), contentElementService);
  }

  static ResponseEntity<?> returnMatches(final Return ret, final Document document,
      final List<ContentElement> matches, final ContentElementService contentElementService) {
    // return mode
    switch (ret){
      case only :
        if (matches.size() > 1)
          throw new NotAcceptableException("More than one content element matches the query");
        // fall-through

      case first :
        if (matches.isEmpty())
          throw new NotFoundException("No matching content element");

        return returnSingleContentElement(document, matches.get(0), contentElementService);

      case all :
        return returnMultipleElementsAsMultipart(document, matches, contentElementService);

      default :
        throw new NotAcceptableException("Unrecognized return mode");
    }
  }

  static List<ContentElement> applyFilters(final List<String> roles, final List<String> acceptHeader,
      final Document document) {
    Stream<ContentElement> elements = document.getContentElements().stream();

    // filter by roles
    if (null != roles)
      elements = elements.filter(ce -> roles.contains(ce.getRole()));

    // filter by accept header
    if (null != acceptHeader && !acceptHeader.contains("*/*"))
      elements = elements.filter(ce -> //
      acceptHeader.stream() //
          .map(h -> {
            try {
              return jakarta.ws.rs.core.MediaType.valueOf(h);
            } catch (Exception e) {
              return null;
            }
          }) //
          .filter(Objects::nonNull)
          .anyMatch(m -> m.isCompatible(ce.getType())));

    return elements.collect(Collectors.toList());
  }

  private static ResponseEntity<MultiValueMap<String, HttpEntity<?>>> returnMultipleElementsAsMultipart(
      final Document document, final List<ContentElement> matches,  final ContentElementService contentElementService) {
    MultiValueMap<String, HttpEntity<?>> mbb = new LinkedMultiValueMap<>(matches.size());

    matches.forEach(ce -> mbb.add(ce.getRole(), returnSingleContentElement(document, ce, contentElementService)));

    return ResponseEntity.ok() //
        .lastModified(document.getDateModified() != null
            ? document.getDateModified().toEpochMilli()
            : document.getDateCreated().toEpochMilli()) //
        .header(HttpHeaders.CONTENT_TYPE, "multipart/mixed") //
        .header(VERSION_TIMESTAMP_HEADER,
            document.getVersionTimestamp() != null ? document.getVersionTimestamp().toString() : "-") //
        .body(mbb);
  }

  private static ResponseEntity<?> returnSingleContentElement(final Document document, final ContentElement contentElement, final ContentElementService contentElementService) {
    // retrieve content
    InputStream contentElementInputStream = contentElementService.getContentElement(document.getDocumentId(),
        contentElement.getId());
    if (contentElementInputStream == null)
      throw new NotFoundException("Object not found in backing store");

    LOGGER.info("add Content response");

    var cdBuilder = ContentDisposition //
      .builder("inline") //
      .name(contentElement.getRole())
      .size(contentElement.getLength());
    if(document.getDateCreated() != null) {
      cdBuilder.creationDate(document.getDateCreated().atZone(ZoneId.systemDefault()));
    }
    if(document.getDateModified() != null) {
      cdBuilder.modificationDate(document.getDateModified().atZone(ZoneId.systemDefault()));
    }
    if (StringUtils.hasText(contentElement.getFileName())) {
      cdBuilder.filename(contentElement.getFileName());
    }
    ContentDisposition contentDisposition = cdBuilder.build();

    // try to canonicalize the algorithm name
    String digestAlgorithmName = contentElement.getDigest().getAlgorithm().name().toLowerCase().replaceAll("_", "-");
    String encodedDigest = Base64.getEncoder().encodeToString(contentElement.getDigest().getBytes());

    return ResponseEntity.ok() //
        .lastModified(document.getDateModified() != null
            ? document.getDateModified().toEpochMilli()
            : document.getDateCreated() != null
                ? document.getDateCreated().toEpochMilli()
                : Instant.now().toEpochMilli()) //
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString()) //
        .header(HttpHeaders.CONTENT_TYPE, contentElement.getType().toString()) //
        .header(HttpHeaders.CONTENT_LENGTH, Long.toString(contentElement.getLength())) //
        .header(VERSION_TIMESTAMP_HEADER,
            document.getVersionTimestamp() != null ? document.getVersionTimestamp().toString() : "-") //
        // add ETag header - yes, the specification proscribes the quotes
        .header(HttpHeaders.ETAG, '"' + digestAlgorithmName + "_" + encodedDigest + '"') //
        .header("Digest", digestAlgorithmName + "=" + encodedDigest) //
        .body(new InputStreamResource( //
            contentElementInputStream, document.getDocumentId() + "/" + contentElement.getId()));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<DocumentDto> createDocumentFromMultipart(
      // mapped using AllRequestPartsMethodArgumentResolver
      final AllRequestParts files, //
      @RequestParam(name = "facets", required = false) final List<String> requestedFacets) throws Exception {
    // try to find the metadata part named __DOC
    DocumentDto doc = docPartFromAllRequestParts(files).orElse(new DocumentDto());

    /*
     * We need to validate at this point lest we create content elements with an invalid id.
     * DocumentResource.create(...) will perform the same validation again, but the validation
     * should be cheap enough not to be a problem.
     */
    documentResource.validate(f -> f.validateCreate(doc));

    if (!StringUtils.hasLength(doc.getDocumentId())) {
      doc.setDocumentId(idGenerationStrategy.createDocumentId());
    }

    List<ContentElement> elements = new ArrayList<>();
    for (MultipartFile file : (Iterable<MultipartFile>) files.getAllParts().stream()
        // ignore __DOC-part(s)
        .filter(f -> !f.getName().equals(DOCUMENT_FORM_ELEMENT_NAME))::iterator) {
      elements.add(//
          contentElementService.createContentElement(doc.getDocumentId(), null, file.getInputStream(), file.getName(),
              file.getOriginalFilename(), file.getContentType(), messageDigest, elements));
    }

    doc.setFacet("contentElements", documentMapper.map(elements, CE_DTO_TYPE));

    // create document and return as status 201 CREATED
    DocumentDto created = documentResource.create(doc, requestedFacets);

    return ResponseEntity//
        .created(URI.create(created.getLink(IanaLinkRelations.SELF).orElseThrow(
            () -> new RuntimeException("self rel not populated")).getHref())) //
        .lastModified(created.getFacetData(mdFacet).orElse(Instant.now()).toEpochMilli()) //
        .body(created);
  }

  /**
   * Try to find a part named {@value #DOCUMENT_FORM_ELEMENT_NAME} and try to map that to a
   * {@link DocumentDto}. Return an empty {@link Optional} if there is no such part.
   *
   * @param files all request parts
   * @return an optional DocumentDto.
   */
  private Optional<DocumentDto> docPartFromAllRequestParts(final AllRequestParts files) {
    return files.getAllParts().stream() //
        .filter(f -> f.getName().equals(DOCUMENT_FORM_ELEMENT_NAME)) //
        .findFirst() //
        .map(f -> {
          try {
            return mapper.readValue(f.getInputStream(), DocumentDto.class);
          } catch (IOException e) {
            throw new NotAcceptableException("__DOC-part is not valid: " + e.getMessage());
          }
        });
  }

  @PostMapping(value = "{documentId}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Timed(description = "add content from multipart", extraTags = {
      "operation", "create", "target", "document-with-content"
  }, value = "eureka.document.create-with-content")
  public DocumentDto add(final HttpServletRequest request, @PathVariable("documentId") final String documentId,
      final AllRequestParts files, // mapped using AllRequestPartsMethodArgumentResolver
      @RequestParam(name = "facets", required = false) final List<String> requestedFacets) throws Exception {
    // preconditions
    documentResource.validateDocumentId(documentId);

    // fetch document
    Document document = documentService.getDocument(documentId).orElseThrow(
        () -> new NotFoundException("Document not found"));

    List<ContentElement> contentElements = document.getContentElements();

    // update list of content elements with newly stored one
    for (MultipartFile file : (Iterable<MultipartFile>) files.getAllParts().stream().filter(
        f -> !f.getName().equals(DOCUMENT_FORM_ELEMENT_NAME))::iterator) {
      contentElements.add(//
          contentElementService.createContentElement(documentId, null, file.getInputStream(), file.getName(),
              file.getOriginalFilename(), file.getContentType(), messageDigest, contentElements));
    }

    // persist document
    return documentResource.update(documentMapper.map(document, DocumentDto.class), document, requestedFacets);
  }

  @PutMapping(value = "{documentID}/content/{content}", consumes = MediaType.ALL_VALUE)
  @Timed(description = "update content element", extraTags = {
      "operation", "update", "target", "content"
  }, value = "eureka.content.update")
  public ResponseEntity<ContentElementDto> update(final HttpServletRequest request, //
      @PathVariable("documentID") final String documentId, //
      @PathVariable("content") final String contentId, final //
      InputStream contentData, // Must not be annotated with @RequestBody for some reason...
      @RequestHeader(name = "Content-Type", required = false) final Optional<String> contentType, //
      @RequestParam(name = "facets", required = false) final List<String> requestedFacets) throws Exception {
    // preconditions
    assertContentExists(documentId, contentId);

    documentResource.validateDocumentId(documentId);

    // fetch document
    Document document = documentService.getDocument(documentId).orElseThrow(
        () -> new NotFoundException("Document not found"));

    List<ContentElement> contentElements = document.getContentElements();

    // Find index of insertion point
    ContentElement toBeReplaced = contentElements.stream().filter(
        e -> e.getId().equals(contentId)).findAny().orElseThrow(
            () -> new NotFoundException("Content element not found"));
    int insertionPoint = contentElements.indexOf(toBeReplaced);
    // create and add new element
    ContentElement contentElement = contentElementService.createContentElement(documentId, null, contentData,
        toBeReplaced.getRole(), toBeReplaced.getFileName(), contentType.orElse(toBeReplaced.getType().toString()),
        messageDigest, contentElements);
    contentElements.set(insertionPoint, contentElement);

    // persist document
    DocumentDto updated = documentResource.update(documentMapper.map(document, DocumentDto.class), document,
        requestedFacets);

    // build result
    ContentElementDto updatedDto = documentMapper.map(contentElement, ContentElementDto.class);
    String digestAlgorithmName = contentElement.getDigest().getAlgorithm().name().toLowerCase().replaceAll("_", "-");
    String encodedDigest = Base64.getEncoder().encodeToString(contentElement.getDigest().getBytes());

    return ResponseEntity.ok() //
        .lastModified(document.getDateModified() != null
            ? document.getDateModified().toEpochMilli()
            : document.getDateCreated().toEpochMilli()) //
        .header(VERSION_TIMESTAMP_HEADER,
            updated.getVersionTimestamp() != null ? updated.getVersionTimestamp().toString() : "-") //
        // add ETag header - yes, the specification proscribes the quotes
        .header(HttpHeaders.ETAG, '"' + digestAlgorithmName + "_" + encodedDigest + '"') //
        .header("Digest", digestAlgorithmName + "=" + encodedDigest) //
        .body(updatedDto);
  }

  @DeleteMapping(value = "{documentID}/content/{element}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  @Timed(description = "delete content element", extraTags = {
      "operation", "delete", "target", "content"
  }, value = "eureka.content.delete")
  public void delete(final HttpServletRequest request, //
      @PathVariable("documentID") final String documentId, //
      @PathVariable("element") final String elementId, //
      @RequestParam(name = "facets", required = false) final List<String> requestedFacets) {
    documentResource.validateDocumentId(documentId);

    Document doc = documentService.getDocument(documentId).orElseThrow(
        () -> new NotFoundException("Document not found"));

    assertContentExists(documentId, elementId);

    List<ContentElement> contentElements = doc.getContentElements();

    contentElements.removeIf(obj -> obj.getId().equals(elementId));

    if (!contentElementService.deleteContentElement(documentId, elementId)) {
      throw new ConflictException(
          "The request could not be completed due to a conflict with the current state of the target resource. ");
    }

    documentResource.update(documentMapper.map(doc, DocumentDto.class), doc, requestedFacets);
  }

  protected void assertContentExists(final String documentId, final String contentId) {
    if (!contentElementService.checkContentExist(documentId, contentId)) {
      throw new NotFoundException("Content not found");
    }
  }
}
