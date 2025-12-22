package com.neverpile.eureka.plugin.metadata.rest;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.atLeastOnce;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.node.ObjectNode;
import com.neverpile.common.authorization.policy.impl.AuthenticationMatcher;
import com.neverpile.eureka.api.DocumentAuthorizationService;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.plugin.metadata.service.Metadata;
import com.neverpile.eureka.plugin.metadata.service.MetadataElement;
import com.neverpile.eureka.plugin.metadata.service.MetadataService;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MetadataAuthorizationTest extends AbstractRestAssuredTest {
  // Must mock the MultiVersioningDocumentService or we will break the app context initialization
  @MockitoBean
  MultiVersioningDocumentService mockDocumentService;

  @MockitoBean
  MetadataService mockMetadataService;

  @MockitoBean
  EventPublisher eventPublisher;

  @Autowired
  MockObjectStoreService mockObjectStoreService;

  @MockitoBean
  DocumentAuthorizationService mockAuthService;

  @MockitoBean
  List<AuthenticationMatcher> authenticationMatchers;

  @BeforeEach
  public void initMock() {
    // provide dummy document
    given(mockDocumentService.getDocument(any())).willAnswer(i -> Optional.of(new Document(i.getArgument(0))));
    given(mockDocumentService.documentExists(any())).willReturn(true);

    // allow create
    given(mockDocumentService.createDocument(any())).willAnswer(i -> i.getArgument(0));

    // store metadata
    given(mockMetadataService.store(any(), any())).willAnswer(i -> i.getArgument(1));
  }

  @Test
  public void testThat_documentRetrieveVerifiesAuthorization() throws JacksonException {
    Metadata metadata = new Metadata();
    metadata.put("foo", new MetadataElement()); // allowed by authorization
    metadata.put("bar", new MetadataElement()); // disallowed
    given(mockMetadataService.get(any())).willAnswer(i -> Optional.of(metadata));

    given(mockAuthService.authorizeSubResourceGet(any(), eq("metadata"), eq("foo"))).willReturn(true);
    given(mockAuthService.authorizeSubResourceGet(any(), eq("metadata"), eq("bar"))).willReturn(false);

    // Create a document
    // @formatter:off
    givenVanillaCall()
    .when()
      .get("/api/v1/documents/{id}", D)
    .then()
      .log().all()
      .body("metadata", hasKey("foo"))
      .body("metadata", not(hasKey("bar"))); // must have been removed from result!
    // @formatter:on

    verify(mockAuthService).authorizeSubResourceGet(any(), eq("metadata"), eq("foo"));
    verify(mockAuthService).authorizeSubResourceGet(any(), eq("metadata"), eq("bar"));
  }

  @Test
  public void testThat_documentCreationWithMetadataVerifiesAuthorization() throws JacksonException {
    given(mockDocumentService.getDocument(any())).willAnswer(i -> Optional.empty());
    given(mockDocumentService.documentExists(any())).willReturn(false);

    // prepare a document
    DocumentDto dto = new DocumentDto();

    MetadataDto mdDto = createTestMetadata();
    dto.setFacet("metadata", mdDto);

    // Create a document
    // @formatter:off
    givenVanillaCall()
        .body(dto)
      .when()
        .post("/api/v1/documents");
    // @formatter:on

    verify(mockAuthService).authorizeSubResourceCreate(any(), eq("metadata"), eq("foo"));
  }

  @Test
  public void testThat_documentUpdateWithMetadataVerifiesAuthorization() throws JacksonException {
    given(mockAuthService.authorizeSubResourceUpdate(any(), any(String[].class))).willReturn(true);
    given(mockAuthService.authorizeSubResourceCreate(any(), any(String[].class))).willReturn(true);
    given(mockAuthService.authorizeSubResourceDelete(any(), any(String[].class))).willReturn(true);
    given(mockAuthService.authorizeSubResourceGet(any(), any(String[].class))).willReturn(true);

    givenVanillaExistingMetadata();

    MetadataDto mdDto = createTestMetadata();
    mdDto.set("baz", new MetadataElementDto()); // unmodified
    mdDto.set("new", new MetadataElementDto()); // to be added

    DocumentDto documentDto = new DocumentDto();
    documentDto.getFacets().put("metadata", mdDto);

    // Create a document
    // @formatter:off
    givenVanillaCall()
      .body(documentDto)
    .when()
      .put("/api/v1/documents/{id}", D);
    // @formatter:on

    verify(mockAuthService).authorizeSubResourceUpdate(any(), eq("metadata"), eq("foo"));
    verify(mockAuthService).authorizeSubResourceDelete(any(), eq("metadata"), eq("bar"));
    verify(mockAuthService).authorizeSubResourceCreate(any(), eq("metadata"), eq("new"));
    verify(mockAuthService, times(3)).authorizeSubResourceGet(any(), eq("metadata"), any());
    verifyNoMoreInteractions(mockAuthService);
  }

  @Test
  public void testThat_metadataRetrieveVerifiesAuthorization() throws JacksonException {
    Metadata metadata = new Metadata();
    metadata.put("foo", new MetadataElement()); // allowed by authorization
    metadata.put("bar", new MetadataElement()); // disallowed
    given(mockMetadataService.get(any())).willAnswer(i -> Optional.of(metadata));

    given(mockAuthService.authorizeSubResourceGet(any(), eq("metadata"), eq("foo"))).willReturn(true);
    given(mockAuthService.authorizeSubResourceGet(any(), eq("metadata"), eq("bar"))).willReturn(false);

    // Create a document
    // @formatter:off
    givenVanillaCall()
    .when()
      .get("/api/v1/documents/{id}/metadata", D)
    .then()
      .log().all()
      .body("$", hasKey("foo"))
      .body("$", not(hasKey("bar"))); // must have been removed from result!
    // @formatter:on

    verify(mockAuthService).authorizeSubResourceGet(any(), eq("metadata"), eq("foo"));
    verify(mockAuthService).authorizeSubResourceGet(any(), eq("metadata"), eq("bar"));
  }

  @Test
  public void testThat_metadataUpdateVerifiesAuthorization() throws JacksonException {
    given(mockAuthService.authorizeSubResourceUpdate(any(), any(String[].class))).willReturn(true);
    given(mockAuthService.authorizeSubResourceCreate(any(), any(String[].class))).willReturn(true);
    given(mockAuthService.authorizeSubResourceDelete(any(), any(String[].class))).willReturn(true);
    given(mockAuthService.authorizeSubResourceGet(any(), any(String[].class))).willReturn(true);

    givenVanillaExistingMetadata();

    MetadataDto mdDto = createTestMetadata();
    mdDto.set("baz", new MetadataElementDto()); // unmodified
    mdDto.set("new", new MetadataElementDto()); // to be added

    // Create a document
    // @formatter:off
    givenVanillaCall()
        .body(mdDto)
      .when()
        .put("/api/v1/documents/{id}/metadata", D);
    // @formatter:on

    verify(mockAuthService).authorizeSubResourceUpdate(any(), eq("metadata"), eq("foo"));
    verify(mockAuthService).authorizeSubResourceDelete(any(), eq("metadata"), eq("bar"));
    verify(mockAuthService).authorizeSubResourceCreate(any(), eq("metadata"), eq("new"));
    verify(mockAuthService, times(3)).authorizeSubResourceGet(any(), eq("metadata"), any());
    verifyNoMoreInteractions(mockAuthService);
  }

  private void givenVanillaExistingMetadata() {
    Metadata metadata = new Metadata();
    metadata.put("foo", new MetadataElement()); // will be updated
    metadata.put("bar", new MetadataElement()); // will be removed
    metadata.put("baz", new MetadataElement()); // unmodified by call!

    given(mockMetadataService.get(any())).willAnswer(i -> Optional.of(metadata));
  }

  @Test
  public void testThat_metadataUpdateRejectedOnDeniedUpdate() throws JacksonException {
    given(mockAuthService.authorizeSubResourceUpdate(any(), any())).willReturn(false);
    given(mockAuthService.authorizeSubResourceCreate(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceDelete(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceGet(any(), any())).willReturn(true);

    givenVanillaExistingMetadata();

    updateMetadataAndExpectDeny(createTestMetadata());
  }

  private void updateMetadataAndExpectDeny(final MetadataDto mdDto) {
    // @formatter:off
    givenVanillaCall()
        .body(mdDto)
      .when()
        .put("/api/v1/documents/{id}/metadata", D)
      .then()
        .statusCode(FORBIDDEN.value());
    // @formatter:on
  }

  @Test
  public void testThat_metadataUpdateRejectedOnDeniedCreate() throws JacksonException {
    given(mockDocumentService.getDocument(any())).willAnswer(i -> Optional.of(new Document(D)));

    given(mockAuthService.authorizeSubResourceUpdate(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceCreate(any(), any())).willReturn(false);
    given(mockAuthService.authorizeSubResourceDelete(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceGet(any(), any())).willReturn(true);

    Metadata metadata = new Metadata(); // start with nothing, try to add "foo"

    given(mockMetadataService.get(any())).willAnswer(i -> Optional.of(metadata));

    MetadataDto mdDto = createTestMetadata();

    updateMetadataAndExpectDeny(mdDto);
  }

  @Test
  public void testThat_metadataUpdateRejectedOnDeniedDelete() throws JacksonException {
    given(mockDocumentService.getDocument(any())).willAnswer(i -> Optional.of(new Document(D)));

    given(mockAuthService.authorizeSubResourceUpdate(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceCreate(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceDelete(any(), any())).willReturn(false);
    given(mockAuthService.authorizeSubResourceGet(any(), any())).willReturn(true);

    givenVanillaExistingMetadata();

    updateMetadataAndExpectDeny(new MetadataDto());
  }

  @Test
  public void testThat_metadataElementRetrieveVerifiesAuthorization() throws JacksonException {
    givenVanillaExistingMetadata();

    given(mockAuthService.authorizeSubResourceGet(any(), eq("metadata"), eq("foo"))).willReturn(true);
    given(mockAuthService.authorizeSubResourceGet(any(), eq("metadata"), eq("bar"))).willReturn(false);

    // @formatter:off
    givenVanillaCall()
    .when()
      .get("/api/v1/documents/{id}/metadata/foo", D)
    .then()
      .statusCode(OK.value())
      .body("$", hasKey("schema")); 
    // @formatter:on

    // @formatter:off
    givenVanillaCall()
    .when()
      .get("/api/v1/documents/{id}/metadata/bar", D)
    .then()
      .statusCode(NOT_FOUND.value());
    // @formatter:on

    verify(mockAuthService, atLeastOnce()).authorizeSubResourceGet(any(), eq("metadata"), eq("foo"));
    verify(mockAuthService, atLeastOnce()).authorizeSubResourceGet(any(), eq("metadata"), eq("bar"));
  }

  @Test
  public void testThat_metadataElementUpdateVerifiesAuthorization() throws JacksonException {
    given(mockAuthService.authorizeSubResourceUpdate(any(), any())).willReturn(true);

    givenVanillaExistingMetadata();

    // @formatter:off
    MetadataElementDto updatedElement = new MetadataElementDto();
    updatedElement.setSchema("test");
    givenVanillaCall()
        .body(updatedElement) 
      .when()
        .put("/api/v1/documents/{id}/metadata/foo", D);
    // @formatter:on

    verify(mockAuthService).authorizeSubResourceUpdate(any(), eq("metadata"), eq("foo"));
  }

  @Test
  public void testThat_metadataElementDeleteVerifiesAuthorization() throws JacksonException {
    given(mockAuthService.authorizeSubResourceDelete(any(), any())).willReturn(true);

    givenVanillaExistingMetadata();

    // @formatter:off
    givenVanillaCall()
      .when()
      .delete("/api/v1/documents/{id}/metadata/foo", D);
    // @formatter:on

    verify(mockAuthService).authorizeSubResourceDelete(any(), eq("metadata"), eq("foo"));
  }

  @Test
  public void testThat_metadataElementCreateVerifiesAuthorization() throws JacksonException {
    given(mockAuthService.authorizeSubResourceDelete(any(), any())).willReturn(true);

    givenVanillaExistingMetadata();

    // @formatter:off
    givenVanillaCall()
      .body(new MetadataElementDto()) 
    .when()
      .put("/api/v1/documents/{id}/metadata/new", D);
    // @formatter:on

    verify(mockAuthService).authorizeSubResourceCreate(any(), eq("metadata"), eq("new"));
  }

  @Test
  public void testThat_metadataElementUpdateRejectedOnDeniedUpdate() throws JacksonException {
    given(mockAuthService.authorizeSubResourceUpdate(any(), any())).willReturn(false);
    given(mockAuthService.authorizeSubResourceCreate(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceDelete(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceGet(any(), any())).willReturn(true);

    givenVanillaExistingMetadata();

    // @formatter:off
    MetadataElementDto updatedElement = new MetadataElementDto();
    updatedElement.setSchema("test");
    givenVanillaCall()
        .body(updatedElement) 
      .when()
        .put("/api/v1/documents/{id}/metadata/foo", D)
      .then()
        .statusCode(FORBIDDEN.value());
    // @formatter:on
  }

  // FIXME
  @Test
  public void testThat_metadataElementDeleteRejectedOnDeniedDelete() throws JacksonException {
    given(mockAuthService.authorizeSubResourceUpdate(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceCreate(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceDelete(any(), any())).willReturn(false);
    given(mockAuthService.authorizeSubResourceGet(any(), any())).willReturn(true);

    givenVanillaExistingMetadata();

    // @formatter:off
    givenVanillaCall()
      .when()
        .delete("/api/v1/documents/{id}/metadata/foo", D)
      .then()
        .statusCode(FORBIDDEN.value());
    // @formatter:on
  }

  // FIXME
  @Test
  public void testThat_metadataElementCreateRejectedOnDeniedCreated() throws JacksonException {
    given(mockAuthService.authorizeSubResourceUpdate(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceCreate(any(), any())).willReturn(false);
    given(mockAuthService.authorizeSubResourceDelete(any(), any())).willReturn(true);
    given(mockAuthService.authorizeSubResourceGet(any(), any())).willReturn(true);

    givenVanillaExistingMetadata();

    // @formatter:off
    givenVanillaCall()
        .body(new MetadataElementDto()) 
      .when()
        .put("/api/v1/documents/{id}/metadata/new", D)
      .then()
        .statusCode(FORBIDDEN.value());
    // @formatter:on
  }

  private MetadataDto createTestMetadata() throws JacksonException {
    MetadataElementDto metadataElement = new MetadataElementDto();
    ObjectNode metadataJson = objectMapper.createObjectNode();
    metadataJson.put("foo", "bar");
    metadataJson.put("bar", "baz");
    metadataElement.setContent(objectMapper.writeValueAsBytes(metadataJson));
    metadataElement.setContentType(MediaType.APPLICATION_JSON_TYPE);
    metadataElement.setSchema("mySchema");
    metadataElement.setDateCreated(Instant.now());
    metadataElement.setDateModified(Instant.now());

    MetadataDto metadata = MetadataDto.with("foo", metadataElement);
    return metadata;
  }
}
