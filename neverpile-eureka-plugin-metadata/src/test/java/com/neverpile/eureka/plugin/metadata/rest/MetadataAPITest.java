package com.neverpile.eureka.plugin.metadata.rest;

import static io.restassured.matcher.RestAssuredMatchers.equalToPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.node.ObjectNode;
import com.neverpile.common.authorization.api.AuthorizationService;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.plugin.metadata.service.Metadata;
import com.neverpile.eureka.plugin.metadata.service.MetadataService;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MetadataAPITest extends AbstractRestAssuredTest {
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
  DocumentIdGenerationStrategy idGenerationStrategy;

  @MockitoBean
  AuthorizationService authorizationService;

  @BeforeEach
  public void init() {
    when(idGenerationStrategy.validateDocumentId(D)).thenReturn(true);
    mockObjectStoreService.streams.clear();
    when(authorizationService.isAccessAllowed(any(), any(), any())).thenReturn(true);
  }

  @Test
  public void testThat_documentCreationWithMetadataWorks() throws JacksonException {
    Instant now = Instant.now();
    BDDMockito.given(mockDocumentService.createDocument(notNull())).willAnswer(i -> {
      Document d = i.getArgument(0);
      d.setDateCreated(now);
      d.setDateModified(now);
      return d;
    });

    ArgumentCaptor<Metadata> storedMetadataC = ArgumentCaptor.forClass(Metadata.class);
    BDDMockito.given(mockMetadataService.store(any(), storedMetadataC.capture())).willAnswer(i -> {
      Metadata m = i.getArgument(1);
      m.forEach((name, element) -> {
        element.setDateCreated(now);
        element.setDateModified(now);
      });
      return m;
    });
    BDDMockito.given(mockMetadataService.get(any())).willAnswer(i -> Optional.of(storedMetadataC.getValue()));

    // prepare a document
    DocumentDto dto = new DocumentDto();

    MetadataDto mdDto = createTestMetadata();
    dto.setFacet("metadata", mdDto);

    // provide an ID
    dto.setDocumentId(D);

    // Create a document
    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
        .body(dto)
        .log().all()
      .when()
        .log().all()
        .post("/api/v1/documents")
      .then()
        .log().all()
        .statusCode(200)
        .body("$", hasKey("dateCreated"))
        .body("$", hasKey("dateModified"))
        .body("$", not(hasKey("contentElements")))
        .body("_links.self.href", Matchers.endsWith("documents/aTestDocument"))
        .body("metadata.foo.schema", equalTo("mySchema"))
        .body("metadata.foo.contentType", equalTo("application/json"))
        .body("metadata.foo.content", equalTo("eyJmb28iOiJiYXIiLCJiYXIiOiJiYXoifQ=="))
        .body("metadata.foo.dateCreated", equalToPath("dateCreated"))
        .body("metadata.foo.dateModified", equalToPath("metadata.foo.dateCreated"))
        .body("metadata.foo._links.self.href", Matchers.endsWith("documents/aTestDocument/metadata/foo"))
        .body("_links.metadata.href", Matchers.endsWith("documents/aTestDocument/metadata"))
        ;
    // @formatter:on

    MetadataDto stored = modelMapper.map(storedMetadataC.getValue(), MetadataDto.class);

    // ignore those for following comparison
    neutralizeLifecycleDates(stored);
    neutralizeLifecycleDates(mdDto);

    assertThat(stored, equalTo(mdDto));
  }

  private void neutralizeLifecycleDates(final MetadataDto stored) {
    stored.getElements().get("foo").setDateCreated(null);
    stored.getElements().get("foo").setDateModified(null);
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

  @Test
  public void testThat_retrievalOfAllDocumentMetadataWorks() throws JacksonException {
    withTestDocumentAndMetadata();

    // Create a document
    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
        .log().all()
      .when()
        .log().all()
        .get("/api/v1/documents/aTestDocument/metadata")
      .then()
        .log().all()
        .statusCode(200)
        .body("foo.schema", equalTo("mySchema"))
        .body("foo.contentType", equalTo("application/json"))
        .body("foo.content", equalTo("eyJmb28iOiJiYXIiLCJiYXIiOiJiYXoifQ=="))
        .body("foo.dateCreated", notNullValue())
        .body("foo.dateModified", notNullValue())
        .body("foo._links.self.href", Matchers.endsWith("documents/aTestDocument/metadata/foo"))
        ;
    // @formatter:on
  }

  private void withTestDocumentAndMetadata() throws JacksonException {
    BDDMockito.given(mockDocumentService.getDocument(notNull())).willReturn(Optional.of(createTestDocument()));
    BDDMockito.given(mockMetadataService.get(any())).willReturn(
        Optional.of(modelMapper.map(createTestMetadata(), Metadata.class)));
  }

  @Test
  public void testThat_replacementOfAllDocumentMetadataWorks() throws JacksonException {
    withTestDocumentAndMetadata();

    Instant now = Instant.now();
    ArgumentCaptor<Metadata> storedMetadataC = ArgumentCaptor.forClass(Metadata.class);
    BDDMockito.given(mockMetadataService.store(any(), storedMetadataC.capture())).willAnswer(i -> {
      Metadata m = i.getArgument(1);
      m.forEach((name, element) -> {
        element.setDateCreated(now);
        element.setDateModified(now);
      });
      return m;
    });
    // BDDMockito.given(mockMetadataService.get(any())).willAnswer(i -> storedMetadataC.getValue());

    MetadataDto m2 = createTestMetadata();
    ObjectNode metadataJson = objectMapper.createObjectNode();
    metadataJson.put("foo", "bar2");
    metadataJson.put("bar", "baz2");
    m2.getElements().get("foo").setContent(objectMapper.writeValueAsBytes(metadataJson));

    MetadataElementDto metadataElement = new MetadataElementDto();
    metadataJson.put("foo", "bar3");
    metadataJson.put("bar", "baz3");
    metadataElement.setContent(objectMapper.writeValueAsBytes(metadataJson));
    metadataElement.setContentType(MediaType.APPLICATION_JSON_TYPE);
    metadataElement.setSchema("otherSchema");
    metadataElement.setDateCreated(now);
    metadataElement.setDateModified(now);

    m2.getElements().put("bar", metadataElement);

    // Create a document
    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
        .body(m2)
        .log().all()
      .when()
        .log().all()
        .put("/api/v1/documents/aTestDocument/metadata")
      .then()
        .log().all()
        .statusCode(200)
        .body("foo.schema", equalTo("mySchema"))
        .body("foo.contentType", equalTo("application/json"))
        .body("foo.content", equalTo("eyJmb28iOiJiYXIyIiwiYmFyIjoiYmF6MiJ9"))
        .body("foo.dateCreated", notNullValue())
        .body("foo.dateModified", notNullValue())
        .body("foo._links.self.href", Matchers.endsWith("documents/aTestDocument/metadata/foo"))
        .body("bar.schema", equalTo("otherSchema"))
        .body("bar.contentType", equalTo("application/json"))
        .body("bar.content", equalTo("eyJmb28iOiJiYXIzIiwiYmFyIjoiYmF6MyJ9"))
        .body("bar.dateCreated", notNullValue())
        .body("bar.dateModified", notNullValue())
        .body("bar._links.self.href", Matchers.endsWith("documents/aTestDocument/metadata/bar"))
        ;
    // @formatter:on

    MetadataDto stored = modelMapper.map(storedMetadataC.getValue(), MetadataDto.class);
    neutralizeLifecycleDates(stored);
    neutralizeLifecycleDates(m2);

    assertThat(stored, equalTo(m2));
  }

  @Override
  protected Document createTestDocument() {
    Document d = super.createTestDocument();
    d.setDocumentId(D);
    return d;
  }

  @Test
  public void testThat_retrievalOfSingleMetadataElementWorks() throws JacksonException {
    withTestDocumentAndMetadata();

    // Create a document
    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
        .log().all()
      .when()
        .log().all()
        .get("/api/v1/documents/aTestDocument/metadata/foo")
      .then()
        .log().all()
        .statusCode(200)
        .body("schema", equalTo("mySchema"))
        .body("contentType", equalTo("application/json"))
        .body("content", equalTo("eyJmb28iOiJiYXIiLCJiYXIiOiJiYXoifQ=="))
        .body("dateCreated", notNullValue())
        .body("dateModified", notNullValue())
        .body("_links.self.href", Matchers.endsWith("documents/aTestDocument/metadata/foo"))
        ;
    // @formatter:on
  }

  @Test
  public void testThat_updateOfSingleMetadataElementWorks() throws JacksonException {

    withTestDocumentAndMetadata();

    ArgumentCaptor<Metadata> storedMetadataC = captureStoredMetadata();

    Instant now = Instant.now();
    MetadataElementDto metadataElement = new MetadataElementDto();
    ObjectNode metadataJson = objectMapper.createObjectNode();
    metadataJson.put("foo", "bar2");
    metadataJson.put("bar", "baz2");
    metadataElement.setContent(objectMapper.writeValueAsBytes(metadataJson));
    metadataElement.setContentType(MediaType.APPLICATION_JSON_TYPE);
    metadataElement.setSchema("otherSchema");
    metadataElement.setDateCreated(now);
    metadataElement.setDateModified(now);

    // Create a document
    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
        .body(metadataElement)
        .log().all()
      .when()
        .log().all()
        .put("/api/v1/documents/aTestDocument/metadata/foo")
      .then()
        .log().all()
        .statusCode(200)
        .body("schema", equalTo("otherSchema"))
        .body("contentType", equalTo("application/json"))
        .body("content", equalTo("eyJmb28iOiJiYXIyIiwiYmFyIjoiYmF6MiJ9"))
        .body("dateCreated", notNullValue())
        .body("dateModified", notNullValue())
        .body("_links.self.href", Matchers.endsWith("documents/aTestDocument/metadata/foo"))
        ;
    // @formatter:on

    MetadataDto stored = modelMapper.map(storedMetadataC.getValue(), MetadataDto.class);
    neutralizeLifecycleDates(stored);
    metadataElement.setDateCreated(null);
    metadataElement.setDateModified(null);

    assertThat(stored.getElements().get("foo"), equalTo(metadataElement));
  }

  @Test
  public void testThat_deleteOfSingleMetadataElementWorks() throws JacksonException {
    withTestDocumentAndMetadata();

    ArgumentCaptor<Metadata> storedMetadataC = captureStoredMetadata();

    // Create a document
    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
        .log().all()
      .when()
        .log().all()
        .delete("/api/v1/documents/aTestDocument/metadata/foo")
      .then()
        .log().all()
        .statusCode(204);
    // @formatter:on

    assertThat(storedMetadataC.getValue().entrySet(), empty());
  }

  private ArgumentCaptor<Metadata> captureStoredMetadata() {
    ArgumentCaptor<Metadata> storedMetadataC = ArgumentCaptor.forClass(Metadata.class);
    BDDMockito.given(mockMetadataService.store(any(), storedMetadataC.capture())).willAnswer(i -> i.getArgument(1));
    return storedMetadataC;
  }

  @Test
  public void testThat_deleteOfNotexistingMetadataElementYieldsError() throws JacksonException {
    withTestDocumentAndMetadata();

    // Create a document
    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
        .log().all()
      .when()
        .log().all()
        .delete("/api/v1/documents/aTestDocument/metadata/baz")
      .then()
        .statusCode(404);
    // @formatter:on
  }
}
