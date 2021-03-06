package com.neverpile.eureka.rest.api.document.content;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.ws.rs.core.MediaType;

import org.apache.commons.fileupload.MultipartStream;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.eureka.api.BaseTestConfiguration;
import com.neverpile.eureka.api.ContentElementIdGenerationStrategy;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.impl.contentservice.SimpleContentElementService;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.model.HashAlgorithm;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BaseTestConfiguration.class)
public class DocumentContentAPITest extends AbstractRestAssuredTest {
  // Must mock the MultiVersioningDocumentService or we will break the app context initialization
  @MockBean
  MultiVersioningDocumentService mockDocumentService;

  @MockBean
  EventPublisher eventPublisher;

  @Autowired
  SimpleContentElementService contentElementService;

  @Autowired
  MockObjectStoreService mockObjectStoreService;

  @MockBean
  DocumentIdGenerationStrategy documentIdGenerationStrategy;

  @MockBean
  ContentElementIdGenerationStrategy contentElementIdGenerationStrategy;

  @Before
  public void reset() {
    AtomicInteger docIdGenerator = new AtomicInteger(42);
    when(documentIdGenerationStrategy.createDocumentId()).thenAnswer(
        (i) -> "TheAnswerIs" + docIdGenerator.getAndIncrement());
    AtomicInteger contentIdGenerator = new AtomicInteger(42);
    when(contentElementIdGenerationStrategy.createContentId(any(), any())).thenAnswer(
        (i) -> "TheAnswerIs" + contentIdGenerator.getAndIncrement());
    when(documentIdGenerationStrategy.validateDocumentId(any())).thenReturn(true);
    when(contentElementIdGenerationStrategy.validateContentId(any(), any(), any())).thenReturn(true);
    mockObjectStoreService.streams.clear();
  }

  /**
   * This method tests the successful creation of a document which already contains an id.
   *
   * @throws Exception
   */
  @Test
  public void testThat_documentCanBeCreatedUsingMultipartAndProvidedId() throws Exception {
    // prepare a document
    DocumentDto dto = new DocumentDto();

    // provide an ID
    dto.setDocumentId(D);

    testDocumentCreation(r -> r.multiPart("__DOC", dto, ContentType.JSON.toString()), D);
  }

  /**
   * This method tests the successful creation of a document with no id.
   *
   * @throws Exception
   */
  @Test
  public void testThat_documentCanBeCreatedUsingMultipartAndNoId() throws Exception {
    // prepare a document
    DocumentDto dto = new DocumentDto();

    testDocumentCreation(r -> r.multiPart("__DOC", dto, ContentType.JSON.toString()), "TheAnswerIs42");
  }

  /**
   * This method tests the successful creation of a document without a __DOC part
   *
   * @throws Exception
   */
  @Test
  public void testThat_documentCanBeCreatedUsingMultipartAndNoDocPart() throws Exception {
    testDocumentCreation(r -> r, "TheAnswerIs42");
  }

  private void testDocumentCreation(final Function<RequestSpecification, RequestSpecification> requestConfigurer,
      final String expectedDocId) {
    Instant then = Instant.now();

    // @formatter:off
    // store it
    ArgumentCaptor<Document> storedDocumentC = ArgumentCaptor.forClass(Document.class);

    BDDMockito
      .given(mockDocumentService.createDocument(storedDocumentC.capture()))
        .willAnswer(i -> i.getArgument(0));

      Document returnedDocument = 
        requestConfigurer.apply(RestAssured.given())
        .accept(ContentType.JSON) 
        .multiPart("base", "foo.txt", "foo".getBytes(), ContentType.TEXT.toString())
        .multiPart("annotation", "foo.xml", "<foo>foobar</foo>".getBytes(), ContentType.XML.toString())
        .multiPart("stuff", "foo.dat", new byte[]{0,1,2,3}, ContentType.BINARY.toString())
        .auth().preemptive().basic("user", "password")
      .when()
        .log().all()
        .post("/api/v1/documents")
      .then()
        .log().all()
        .statusCode(is(in(Arrays.asList(200, 201))))
        .contentType(ContentType.JSON)
        .body("documentId", equalTo(expectedDocId))
        .body("contentElements.size()", equalTo(3))
        .body("contentElements[0].id", equalTo("TheAnswerIs42"))
        .body("contentElements[0].role", equalTo("base"))
        .body("contentElements[0].fileName", equalTo("foo.txt"))
        .body("contentElements[0].type", equalTo(MediaType.TEXT_PLAIN))
        .body("contentElements[0].length", equalTo(3))
        .body("contentElements[0].encryption", equalTo("SHARED"))
        .body("contentElements[0].digest.algorithm", equalTo("SHA-256"))
        .body("contentElements[0].digest.bytes", equalTo("LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564="))
        .body("contentElements[1].id", equalTo("TheAnswerIs43"))
        .body("contentElements[1].role", equalTo("annotation"))
        .body("contentElements[1].fileName", equalTo("foo.xml"))
        .body("contentElements[1].type", equalTo(MediaType.APPLICATION_XML))
        .body("contentElements[1].length", equalTo(17))
        .body("contentElements[1].encryption", equalTo("SHARED"))
        .body("contentElements[1].digest.algorithm", equalTo("SHA-256"))
        .body("contentElements[1].digest.bytes", equalTo("STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0="))
        .body("contentElements[2].id", equalTo("TheAnswerIs44"))
        .body("contentElements[2].role", equalTo("stuff"))
        .body("contentElements[2].fileName", equalTo("foo.dat"))
        .body("contentElements[2].type", equalTo(MediaType.APPLICATION_OCTET_STREAM))
        .body("contentElements[2].length", equalTo(4))
        .body("contentElements[2].encryption", equalTo("SHARED"))
        .body("contentElements[2].digest.algorithm", equalTo("SHA-256"))
        .body("contentElements[2].digest.bytes", equalTo("BU7ewdAhH2JP7Qy8qdT5QAsOSRxDdCryxbCr6/DJkNg="))
        .extract().as(Document.class);

    // verify returned document
    Instant now = Instant.now();
    assertThat(returnedDocument.getDocumentId(), equalTo(expectedDocId));
    assertThat(returnedDocument.getDateCreated(), allOf(greaterThanOrEqualTo(then), lessThanOrEqualTo(now)));

    // verify stored document
    Document storedDocument = storedDocumentC.getValue();
    assertThat(storedDocument.getDocumentId(), equalTo(expectedDocId));
    assertThat(storedDocument.getDateCreated(), allOf(greaterThanOrEqualTo(then), lessThanOrEqualTo(now)));

    // FIXME: cannot currently be checked as the representation changes
    // assertThat(storedDocument.getMetadata(), contains(metadata));

    assertThat(storedDocument.getContentElements(), hasSize(3));

    // verify stored streams
    assertThat(mockObjectStoreService.streams.size(), equalTo(3));
    assertThat(mockObjectStoreService.streams, hasEntry(
        ObjectName.of("document", expectedDocId, "TheAnswerIs42"),
        "foo".getBytes()));
    assertThat(mockObjectStoreService.streams, hasEntry(
        ObjectName.of("document", expectedDocId, "TheAnswerIs43"),
        "<foo>foobar</foo>".getBytes()));
    assertThat(mockObjectStoreService.streams, hasEntry(
        ObjectName.of("document", expectedDocId, "TheAnswerIs44"),
        new byte[]{0,1,2,3}));
    // @formatter:on
  }

  /**
   * This method tests the successful creation of a document without a __DOC part
   *
   * @throws Exception
   */
  @Test
  public void testThat_documentCanBeCreatedUsingMultipartWithMissingContentTypeOnDOCPart() throws Exception {

    // @formatter:off
    BDDMockito
      .given(mockDocumentService.createDocument(any()))
        .willAnswer(i -> i.getArgument(0));
    
    Document returnedDocument = 
        ((Function<RequestSpecification, RequestSpecification>) r -> r).apply(RestAssured.given())
        .accept(ContentType.JSON) 
        .multiPart("__DOC", "{\"documentId\": \"myProvidedId\"}")
        .multiPart("base", "foo.txt", "foo".getBytes(), ContentType.TEXT.toString())
        .auth().preemptive().basic("user", "password")
      .when()
        .log().all()
        .post("/api/v1/documents")
      .then()
        .log().all()
        .statusCode(is(in(Arrays.asList(200, 201))))
        .contentType(ContentType.JSON)
        .body("documentId", equalTo("myProvidedId"))
        .body("contentElements.size()", equalTo(1))
        .extract().as(Document.class);
    
    // verify returned document
    assertThat(returnedDocument.getDocumentId()).isEqualTo("myProvidedId");
    
    // @formatter:on
  }

  @Test
  public void testThat_contentElementsCanBeRetrievedById() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));

    // retrieve and verify parts
    RestAssured
        .given()
          .log().all()
          .accept(ContentType.ANY)
          .auth().preemptive().basic("user", "password")
        .when().get("/api/v1/documents/{documentID}/content/{part}", D,
            doc.getContentElements().get(0).getId())
        .then()
          .log().all()
          .statusCode(200)
          .contentType("text/plain")
          .header("Content-Disposition", Matchers.startsWith("inline; name=\"part\"; filename=\"foo.txt\""))
          .header("Digest", Matchers.equalTo("sha-256=LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564="))
          .header("ETag", Matchers.equalTo("\"sha-256_LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564=\""))          
          .body(equalTo("foo"));

    RestAssured
        .given()
          .accept(ContentType.ANY)
          .auth().preemptive().basic("user", "password")
        .when().get("/api/v1/documents/{documentID}/content/{part}", D,
            doc.getContentElements().get(1).getId())
        .then()
          .statusCode(200)
          .contentType("application/xml")
          .header("Content-Disposition",  Matchers.startsWith("inline; name=\"annotations\"; filename=\"foo.xml\""))
          .header("Digest", Matchers.equalTo("sha-256=STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0="))
          .header("ETag", Matchers.equalTo("\"sha-256_STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0=\""))
          .body(equalTo("<foo>foobar</foo>"));

    byte[] bodyBytes = RestAssured
        .given()
          .accept(ContentType.ANY)
          .auth().preemptive().basic("user", "password")
        .when().get("/api/v1/documents/{documentID}/content/{part}", D,
            doc.getContentElements().get(2).getId())
        .then()
          .statusCode(200)
          .contentType("application/octet-stream")
          .header("Content-Disposition",  Matchers.startsWith("inline; name=\"stuff\"; filename=\"foo.dat\""))
          .header("Digest", Matchers.equalTo("sha-256=STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0="))
          .header("ETag", Matchers.equalTo("\"sha-256_STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0=\""))
          .extract().asByteArray();

    // can't use rest-assured body check at it messes up binary content
    assertThat(bodyBytes, equalTo(new byte[] {0, 1, 2, 3}));

    // @formatter:on
  }

  @Test
  public void testThat_contentQueryReturnsSinglePart() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));

    // retrieve and verify parts
    RestAssured
        .given()
          .log().all()
          .accept(ContentType.ANY)
          .auth().preemptive().basic("user", "password")
        .when()
          .queryParam("role", "part")
          .queryParam("return", "first")
          .get("/api/v1/documents/{documentID}/content", D)
        .then()
          .log().all()
          .statusCode(200)
          .contentType("text/plain")
          .header("Content-Disposition", Matchers.startsWith("inline; name=\"part\"; filename=\"foo.txt\""))
          .header("Digest", Matchers.equalTo("sha-256=LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564="))
          .header("ETag", Matchers.equalTo("\"sha-256_LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564=\""))
          .body(equalTo("foo"));
    // @formatter:on
  }

  @Test
  public void testThat_contentQueryReturnsFirstPartWithReturnFirst() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));
    
    // retrieve and verify parts
    RestAssured
    .given()
      .log().all()
      .accept(ContentType.ANY)
      .auth().preemptive().basic("user", "password")
    .when()
      .queryParam("return", "first")
      .get("/api/v1/documents/{documentID}/content", D)
    .then()
      .log().all()
      .statusCode(200)
      .contentType("text/plain")
      .header("Content-Disposition", Matchers.startsWith("inline; name=\"part\"; filename=\"foo.txt\""))
      .body(equalTo("foo"));
    // @formatter:on
  }

  @Test
  public void testThat_contentQueryReturnsOnlyPartWithReturnOnly() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));
    
    // retrieve and verify parts
    RestAssured
    .given()
      .log().all()
      .accept(ContentType.ANY)
      .auth().preemptive().basic("user", "password")
    .when()
      .queryParam("role", "annotations")
      .queryParam("return", "only")
      .get("/api/v1/documents/{documentID}/content", D)
    .then()
      .log().all()
      .statusCode(200)
      .contentType("application/xml")
      .header("Content-Disposition", Matchers.startsWith("inline; name=\"annotations\"; filename=\"foo.xml\""))
      .body(equalTo("<foo>foobar</foo>"));
    // @formatter:on
  }

  @Test
  public void testThat_contentQueryReturnsAllPartsWithReturnAll() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));
    
    // retrieve and verify parts
    ExtractableResponse<Response> response = RestAssured
      .given()
        .log().all()
        .accept(ContentType.ANY)
        .auth().preemptive().basic("user", "password")
      .when()
        .queryParam("return", "all")
        .get("/api/v1/documents/{documentID}/content", D)
      .then()
        .log().all()
        .statusCode(200)
        .contentType("multipart/mixed").extract();
    
    byte[] responseBytes = response.response().asByteArray();
    // @formatter:on

    MediaType mt = MediaType.valueOf(response.header("Content-Type"));

    MultipartStream ms = new MultipartStream(new ByteArrayInputStream(responseBytes),
        mt.getParameters().get("boundary").getBytes(), 1024, null);

    String[] headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Disposition: inline; name=\"part\"; filename=\"foo.txt\""));
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Type: text/plain"));
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Length: 3"));
    assertThat(headers).anyMatch(s -> s.startsWith("ETag: \"sha-256_LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564=\""));
    assertThat(headers).anyMatch(s -> s.startsWith("Digest: sha-256=LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564="));

    ByteArrayOutputStream body = new ByteArrayOutputStream();
    ms.readBodyData(body);
    assertThat(body.toByteArray()).isEqualTo("foo".getBytes());

    headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(
        s -> s.startsWith("Content-Disposition: inline; name=\"annotations\"; filename=\"foo.xml\""));
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Type: application/xml"));
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Length: 17"));
    assertThat(headers).anyMatch(s -> s.startsWith("ETag: \"sha-256_STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0=\""));
    assertThat(headers).anyMatch(s -> s.startsWith("Digest: sha-256=STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0="));

    body = new ByteArrayOutputStream();
    ms.readBodyData(body);
    assertThat(body.toByteArray()).isEqualTo("<foo>foobar</foo>".getBytes());

    headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(
        s -> s.startsWith("Content-Disposition: inline; name=\"stuff\"; filename=\"foo.dat\""));
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Type: application/octet-stream"));
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Length: 4"));
    assertThat(headers).anyMatch(s -> s.startsWith("ETag: \"sha-256_STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0=\""));
    assertThat(headers).anyMatch(s -> s.startsWith("Digest: sha-256=STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0="));

    body = new ByteArrayOutputStream();
    ms.readBodyData(body);
    assertThat(body.toByteArray()).contains(0, 1, 2, 3);

    headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(
        s -> s.startsWith("Content-Disposition: inline; name=\"stuff\"; filename=\"fox.txt\""));
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Type: application/octet-stream"));
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Length: 44"));
    assertThat(headers).anyMatch(
        s -> s.startsWith("ETag: \"sha-256_7d38b5cd25a2baf85ad3bb5b9311383e671a8a142eb302b324d4a5fba8748c69\""));
    assertThat(headers).anyMatch(
        s -> s.startsWith("Digest: sha-256=7d38b5cd25a2baf85ad3bb5b9311383e671a8a142eb302b324d4a5fba8748c69"));

    body = new ByteArrayOutputStream();
    ms.readBodyData(body);
    assertThat(body.toByteArray()).isEqualTo("The quick brown fox jumped over the lazy dog".getBytes());
  }

  @Test
  public void testThat_contentQueryReturnsMatchingPartsByRole() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));
    
    // retrieve and verify parts
    ExtractableResponse<Response> response = RestAssured
        .given()
        .log().all()
        .accept(ContentType.ANY)
        .auth().preemptive().basic("user", "password")
        .when()
        .queryParam("role", "stuff")
        .queryParam("return", "all")
        .get("/api/v1/documents/{documentID}/content", D)
        .then()
        .log().all()
        .statusCode(200)
        .contentType("multipart/mixed").extract();
    
    byte[] responseBytes = response.response().asByteArray();
    // @formatter:on

    MediaType mt = MediaType.valueOf(response.header("Content-Type"));

    MultipartStream ms = new MultipartStream(new ByteArrayInputStream(responseBytes),
        mt.getParameters().get("boundary").getBytes(), 1024, null);

    String[] headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(
        s -> s.startsWith("Content-Disposition: inline; name=\"stuff\"; filename=\"foo.dat\""));

    ms.discardBodyData();

    headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(
        s -> s.startsWith("Content-Disposition: inline; name=\"stuff\"; filename=\"fox.txt\""));
  }

  @Test
  public void testThat_contentQueryReturnsMatchingPartsByTypeWithMultipleRoles() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));
    
    // retrieve and verify parts
    ExtractableResponse<Response> response = RestAssured
        .given()
        .log().all()
        .accept(ContentType.ANY)
        .auth().preemptive().basic("user", "password")
        .when()
        .queryParam("role", "part", "annotations")
        .queryParam("return", "all")
        .get("/api/v1/documents/{documentID}/content", D)
        .then()
        .log().all()
        .statusCode(200)
        .contentType("multipart/mixed").extract();
    
    byte[] responseBytes = response.response().asByteArray();
    // @formatter:on

    MediaType mt = MediaType.valueOf(response.header("Content-Type"));

    MultipartStream ms = new MultipartStream(new ByteArrayInputStream(responseBytes),
        mt.getParameters().get("boundary").getBytes(), 1024, null);

    String[] headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Disposition: inline; name=\"part\"; filename=\"foo.txt\""));

    ms.discardBodyData();

    headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(
        s -> s.startsWith("Content-Disposition: inline; name=\"annotations\"; filename=\"foo.xml\""));
  }


  @Test
  public void testThat_contentQueryReturnsMatchingPartsByType() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));
    
    // retrieve and verify parts
    ExtractableResponse<Response> response = RestAssured
      .given()
        .log().all()
        .accept(ContentType.XML)
        .auth().preemptive().basic("user", "password")
      .when()
        .queryParam("return", "all")
        .get("/api/v1/documents/{documentID}/content", D)
      .then()
        .log().all()
        .statusCode(200)
        .contentType("multipart/mixed").extract();
    
    byte[] responseBytes = response.response().asByteArray();
    // @formatter:on

    MediaType mt = MediaType.valueOf(response.header("Content-Type"));

    MultipartStream ms = new MultipartStream(new ByteArrayInputStream(responseBytes),
        mt.getParameters().get("boundary").getBytes(), 1024, null);

    String[] headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(
        s -> s.startsWith("Content-Disposition: inline; name=\"annotations\"; filename=\"foo.xml\""));
  }

  @Test
  public void testThat_contentQueryReturnsMatchingPartsByTypeWithMultipleTypes() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));
    
    // retrieve and verify parts
    ExtractableResponse<Response> response = RestAssured
      .given()
        .log().all()
        .accept("application/xml, text/plain")
        .auth().preemptive().basic("user", "password")
      .when()
        .queryParam("role", "part,annotations")
        .queryParam("return", "all")
        .get("/api/v1/documents/{documentID}/content", D)
      .then()
        .log().all()
        .statusCode(200)
        .contentType("multipart/mixed").extract();
    
    byte[] responseBytes = response.response().asByteArray();
    // @formatter:on

    MediaType mt = MediaType.valueOf(response.header("Content-Type"));

    MultipartStream ms = new MultipartStream(new ByteArrayInputStream(responseBytes),
        mt.getParameters().get("boundary").getBytes(), 1024, null);

    String[] headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(s -> s.startsWith("Content-Disposition: inline; name=\"part\"; filename=\"foo.txt\""));

    ms.discardBodyData();

    headers = ms.readHeaders().split("\r\n");
    assertThat(headers).anyMatch(
        s -> s.startsWith("Content-Disposition: inline; name=\"annotations\"; filename=\"foo.xml\""));
  }

  @Test
  public void testThat_contentQueryFailsOnMissingPart() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));
    
    // retrieve and verify parts
    RestAssured
        .given()
          .log().all()
          .accept(ContentType.ANY)
          .auth().preemptive().basic("user", "password")
        .when()
          .queryParam("role", "doesntexist")
          .queryParam("return", "first")
          .get("/api/v1/documents/{documentID}/content", D)
        .then()
          .log().all()
          .statusCode(404)
          .body("message", equalTo("No matching content element"));
    // @formatter:on
  }

  @Test
  public void testThat_contentQueryFailsOnMultipleMatchesWithReturnOnly() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));
    
    // retrieve and verify parts
    RestAssured
        .given()
          .log().all()
          .accept(ContentType.ANY)
          .auth().preemptive().basic("user", "password")
        .when()
          .queryParam("return", "only")
          .get("/api/v1/documents/{documentID}/content", D)
        .then()
          .log().all()
          .statusCode(406)
          .body("message", equalTo("More than one content element matches the query"));
    // @formatter:on
  }

  private Document createTestDocumentWithContent() {
    Document doc = createTestDocument();
    doc.setVersionTimestamp(Instant.ofEpochMilli(42L));

    doc.setDocumentId(D);

    ContentElement ce0 = new ContentElement();
    ce0.setContentElementId("2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae");
    ce0.setType(MediaType.TEXT_PLAIN_TYPE);
    ce0.setEncryption(EncryptionType.SHARED);
    ce0.setDigest(new Digest());
    ce0.getDigest().setAlgorithm(HashAlgorithm.SHA_256);
    ce0.getDigest().setBytes(Base64.getDecoder().decode("LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564="));
    ce0.setFileName("foo.txt");
    ce0.setLength(3);
    ce0.setRole("part");

    ContentElement ce1 = new ContentElement();
    ce1.setContentElementId("4938d873b6755092912b54f97033052206192a4eaae5ce9a4f235a1067d04b0d");
    ce1.setType(MediaType.APPLICATION_XML_TYPE);
    ce1.setEncryption(EncryptionType.SHARED);
    ce1.setDigest(new Digest());
    ce1.getDigest().setAlgorithm(HashAlgorithm.SHA_256);
    ce1.getDigest().setBytes(Base64.getDecoder().decode("STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0="));
    ce1.setFileName("foo.xml");
    ce1.setLength(17);
    ce1.setRole("annotations");

    ContentElement ce2 = new ContentElement();
    ce2.setContentElementId("content054edec1d0211f624fed0cbca9d4f9400b0e491c43742af2c5b0abebf0c990d8");
    ce2.setType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
    ce2.setEncryption(EncryptionType.SHARED);
    ce2.setDigest(new Digest());
    ce2.getDigest().setAlgorithm(HashAlgorithm.SHA_256);
    ce2.getDigest().setBytes(Base64.getDecoder().decode("STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0="));
    ce2.setFileName("foo.dat");
    ce2.setLength(4);
    ce2.setRole("stuff");

    ContentElement ce3 = new ContentElement();
    ce3.setContentElementId("7d38b5cd25a2baf85ad3bb5b9311383e671a8a142eb302b324d4a5fba8748c69");
    ce3.setType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
    ce3.setEncryption(EncryptionType.SHARED);
    ce3.setDigest(new Digest());
    ce3.getDigest().setAlgorithm(HashAlgorithm.SHA_256);
    ce3.getDigest().setBytes(
        Base64.getDecoder().decode("7d38b5cd25a2baf85ad3bb5b9311383e671a8a142eb302b324d4a5fba8748c69"));
    ce3.setFileName("fox.txt");
    ce3.setLength(44);
    ce3.setRole("stuff");

    ArrayList<ContentElement> contentElements = new ArrayList<ContentElement>();
    contentElements.add(ce0);
    contentElements.add(ce1);
    contentElements.add(ce2);
    contentElements.add(ce3);
    doc.setContentElements(contentElements);

    primeObjectStore(doc);

    return doc;
  }

  /**
   * Prime the object store with content matching the elements defined
   * {@link #createTestDocumentWithContent()}.
   * 
   * @param doc
   */
  private void primeObjectStore(final Document doc) {
    byte[][] content = new byte[][]{
        "foo".getBytes(), //
        "<foo>foobar</foo>".getBytes(), //
        new byte[]{
            0, 1, 2, 3
        }, //
        "The quick brown fox jumped over the lazy dog".getBytes()
    };
    Iterator<byte[]> i = Arrays.asList(content).iterator();
    doc.getContentElements().forEach(
        c -> mockObjectStoreService.streams.put(ObjectName.of("document", D, c.getId()), i.next()));
  }

  /**
   * TODO - Tests if the deleteDocument method is called on the HTTP delete call.
   *
   * @throws Exception
   */
  @Test
  public void testThat_deleteOfDocumentPropagatesToContentElements() throws Exception {
    Document doc = createTestDocumentWithContent();

    assertThat(mockObjectStoreService.streams).isNotEmpty();
    // @formatter:off
    // retrieve it
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));
    BDDMockito.given(mockDocumentService.documentExists(notNull())).willReturn(true);

    // @formatter:off
    given()
      .auth().preemptive().basic("user", "password")
    .when()
      .delete("/api/v1/documents/{document}", D)
    .then()
      .log().all()
      .statusCode(204);
    // @formatter:on

    verify(mockDocumentService, times(1)).deleteDocument(D);

    // must have all been deleted!
    assertThat(mockObjectStoreService.streams).isEmpty();
  }

  @Test
  public void testThat_contentElementsCanBeAddedFromMultipart() throws Exception {
    Document doc = createTestDocumentWithContent();
    int contentCount = doc.getContentElements().size();

    // @formatter:off
    BDDMockito
      .given(mockDocumentService.update(notNull()))
      .willAnswer(i -> Optional.of(i.getArgument(0)));
    BDDMockito
      .given(mockDocumentService.getDocument(D))
      .willReturn(Optional.of(doc));

    RestAssured
        .given()
          .accept(ContentType.JSON)
          .multiPart("part", "foo.xml", "<foo>foobar</foo>".getBytes(), ContentType.XML.toString())
          .auth().preemptive().basic("user", "password")
         .when()
          .post("/api/v1/documents/{document}/content", doc.getDocumentId())
        .then()
          .log().all()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("documentId", equalTo(D))
          .body("contentElements.size()", equalTo((contentCount + 1)))
          .body("contentElements[4].id", equalTo("TheAnswerIs42"))
          .body("contentElements[4].role", equalTo("part"))
          .body("contentElements[4].fileName", equalTo("foo.xml"))
          .body("contentElements[4].type", equalTo(MediaType.APPLICATION_XML))
          .body("contentElements[4].length", equalTo(17))
          .body("contentElements[4].encryption", equalTo("SHARED"))
          .body("contentElements[4].digest.algorithm", equalTo("SHA-256"))
          .body("contentElements[4].digest.bytes", equalTo("STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0="));
    // @formatter:on

    assertThat(mockObjectStoreService.streams).hasSize(contentCount + 1);
  }

  @Test
  public void testThat_contentElementsCanBeUpdated() throws Exception {
    Document doc = createTestDocumentWithContent();
    int contentCount = doc.getContentElements().size();

    // @formatter:off
    BDDMockito
      .given(mockDocumentService.update(notNull()))
      .willAnswer(i -> Optional.of(i.getArgument(0)));
    BDDMockito
      .given(mockDocumentService.getDocument(D))
      .willReturn(Optional.of(doc));

    RestAssured
        .given()
          .accept(ContentType.JSON)
          .body("<bar>Hello, world!</bar>".getBytes())
          .contentType("application/x-foo")
          .auth().preemptive().basic("user", "password")
        .when()
          .put("/api/v1/documents/{document}/content/{content}", 
              doc.getDocumentId(), "4938d873b6755092912b54f97033052206192a4eaae5ce9a4f235a1067d04b0d")
        .then()
          .log().all()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("id", equalTo("TheAnswerIs42"))
          .body("role", equalTo("annotations"))
          .body("fileName", equalTo("foo.xml"))
          .body("type", startsWith("application/x-foo"))
          .body("length", equalTo(24))
          .body("encryption", equalTo("SHARED"))
          .body("digest.algorithm", equalTo("SHA-256"))
          .body("digest.bytes", equalTo("DOmUMVitclB+nLT1VJ+x2BGgFhTWU3k7o9KV5Ijl3qw="))
          ;
    // @formatter:on

    assertThat(mockObjectStoreService.streams).hasSize(contentCount + 1);
    assertThat(new String(
        mockObjectStoreService.streams.get(ObjectName.of("document", "aTestDocument", "TheAnswerIs42")))).isEqualTo(
            "<bar>Hello, world!</bar>");
  }

  @Test
  public void testThat_contentElementCanBeDeleted() throws Exception {
    Document doc = createTestDocumentWithContent();
    int contentCount = doc.getContentElements().size();
    String contentId = "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae";

    ArgumentCaptor<Document> storedDocumentC = ArgumentCaptor.forClass(Document.class);

    BDDMockito.given(mockDocumentService.update(storedDocumentC.capture())).willAnswer(
        i -> Optional.of(i.getArgument(0)));
    BDDMockito.given(mockDocumentService.getDocument(D)).willReturn(Optional.of(doc));

    mockObjectStoreService.put(ObjectName.of("document", D, contentId), ObjectStoreService.NEW_VERSION,
        new ByteArrayInputStream("foo".getBytes()));

    // @formatter:off
    RestAssured
        .given()
          .auth().preemptive().basic("user", "password")
          .accept(ContentType.ANY)
         .when()
         .log().all()
          .delete("/api/v1/documents/{document}/content/2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae", doc.getDocumentId())
        .then()
          .statusCode(204);

    assertThat(storedDocumentC.getValue().getContentElements().size(), equalTo(--contentCount));
    assertThat(mockObjectStoreService.get(ObjectName.of("document", D, contentId)), nullValue());
  }

  // @formatter:off

}
