package com.neverpile.eureka.rest.api.document;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.eureka.api.BaseTestConfiguration;
import com.neverpile.eureka.api.ContentElementIdGenerationStrategy;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.impl.contentservice.SimpleContentElementService;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.model.HashAlgorithm;
import com.neverpile.eureka.rest.api.document.content.ContentElementFacet;
import com.neverpile.eureka.rest.api.document.content.ContentElementResource;
import com.neverpile.eureka.rest.api.document.core.CreationDateFacet;
import com.neverpile.eureka.rest.api.document.core.IdFacet;
import com.neverpile.eureka.rest.api.document.core.ModificationDateFacet;
import com.neverpile.eureka.rest.configuration.ModelMapperConfiguration;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BaseTestConfiguration.class)
public class DocumentAPITest extends AbstractRestAssuredTest {
  @TestConfiguration
  @Import({
      ModelMapperConfiguration.class, SimpleContentElementService.class, ContentElementFacet.class,
      ContentElementResource.class, IdFacet.class, ModificationDateFacet.class, CreationDateFacet.class,
      DocumentResource.class
  })
  public static class ServiceConfig {
  }

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
   * This method tests the successful creation of a unique id. It also tests if the id matches the
   * prescribed pattern (regex)
   */
  @Test
  public void testThat_documentCanBeCreatedWithGeneratedId() {
    DocumentDto dto = new DocumentDto();

    // @formatter:off
    ArgumentCaptor<Document> storedDocumentC = ArgumentCaptor.forClass(Document.class);

    BDDMockito
      .given(mockDocumentService.createDocument(storedDocumentC.capture()))
        .willAnswer(i -> i.getArgument(0));

    // store document
    DocumentDto resDoc = given()
        .accept(ContentType.JSON)
        .multiPart("doc", dto, ContentType.JSON.toString())
        .auth().preemptive().basic("user", "password")
      .when()
        .log().all()
        .post("/api/v1/documents")
        .as(DocumentDto.class);


    assertThat(resDoc.getDocumentId(), equalTo("TheAnswerIs42"));

    // store another one
    Document res2Doc = given()
        .accept(ContentType.JSON)
        .multiPart("doc", dto, ContentType.JSON.toString())
        .auth().preemptive().basic("user", "password")
      .when()
        .post("/api/v1/documents")
        .as(Document.class);
    // @formatter:on
    assertThat(res2Doc.getDocumentId(), equalTo("TheAnswerIs43"));
  }

  @Test
  public void testThat_documentCanBeRetrievedAsJSON() throws Exception {
    Instant then = Instant.now();

    Document doc = createTestDocumentWithContent();

    // @formatter:off
    // retrieve it
    BDDMockito
      .given(mockDocumentService.getDocument(D))
        .willReturn(Optional.of(doc));

    Document returnedDocument = RestAssured
      .given()
        .accept(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{document}", D)
      .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("documentId", equalTo(D))
        .body("contentElements.size()", equalTo(3))
        .body("contentElements[0].id", equalTo("2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae"))
        .body("contentElements[0].role", equalTo("part"))
        .body("contentElements[0].fileName", equalTo("foo.txt"))
        .body("contentElements[0].type", equalTo(MediaType.TEXT_PLAIN))
        .body("contentElements[0].length", equalTo(3))
        .body("contentElements[0].encryption", equalTo("SHARED"))
        .body("contentElements[0].digest.algorithm", equalTo("SHA-256"))
        .body("contentElements[0].digest.bytes", equalTo("LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564="))
        .body("contentElements[1].id", equalTo("4938d873b6755092912b54f97033052206192a4eaae5ce9a4f235a1067d04b0d"))
        .body("contentElements[1].role", equalTo("annotations"))
        .body("contentElements[1].fileName", equalTo("foo.xml"))
        .body("contentElements[1].type", equalTo(MediaType.APPLICATION_XML))
        .body("contentElements[1].length", equalTo(17))
        .body("contentElements[1].encryption", equalTo("SHARED"))
        .body("contentElements[1].digest.algorithm", equalTo("SHA-256"))
        .body("contentElements[1].digest.bytes", equalTo("STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0="))
        .body("contentElements[2].id", equalTo("content054edec1d0211f624fed0cbca9d4f9400b0e491c43742af2c5b0abebf0c990d8"))
        .body("contentElements[2].role", equalTo("stuff"))
        .body("contentElements[2].fileName", equalTo("foo.dat"))
        .body("contentElements[2].type", equalTo(MediaType.APPLICATION_OCTET_STREAM))
        .body("contentElements[2].length", equalTo(4))
        .body("contentElements[2].encryption", equalTo("SHARED"))
        .body("contentElements[2].digest.algorithm", equalTo("SHA-256"))
        .body("contentElements[2].digest.bytes", equalTo("STjYc7Z1UJKRK1T5cDMFIgYZKk6q5c6aTyNaEGfQSw0="))
        .extract().as(Document.class);
    // @formatter:on

    // verify returned document
    Instant now = Instant.now();
    assertThat(returnedDocument.getDocumentId(), equalTo(D));
    assertThat(returnedDocument.getDateCreated(), allOf(greaterThanOrEqualTo(then), lessThanOrEqualTo(now)));

  }

  private Document createTestDocumentWithContent() {
    Document doc = createTestDocument();

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

    ArrayList<ContentElement> contentElements = new ArrayList<ContentElement>();
    contentElements.add(ce0);
    contentElements.add(ce1);
    contentElements.add(ce2);
    doc.setContentElements(contentElements);
    return doc;
  }

  @Test
  public void testThat_nonexistentDocumentYields404() throws Exception {
    // retrieve nonexistent
    // @formatter:off
    given()
      .accept(ContentType.JSON)
      .auth().preemptive().basic("user", "password")
    .when()
        .get("/api/v1/documents/{document}", "IDoNotExist.SorryBoutThat")
    .then()
        .log().all()
        .statusCode(404);
    // @formatter:on
  }

  @Test
  public void testThat_unauthorizedAccessYields401() throws Exception {
    // retrieve nonexistent
    // @formatter:off
    given()
      .accept(ContentType.JSON)
    .when()
        .get("/api/v1/documents/{document}", "IDoNotExist.SorryBoutThat")
    .then()
        .statusCode(401);
    // @formatter:on
  }

  /**
   * TODO - Tests if the deleteDocument method is called on the HTTP delete call.
   *
   * @throws Exception
   */
  @Test
  public void testThat_deleteFunctionIsCalled() throws Exception {

    Document doc = new Document();
    doc.setDocumentId(D);

    // @formatter:off
    BDDMockito
      .given(mockDocumentService.getDocument(D))
        .willReturn(Optional.of(doc));
    BDDMockito
      .given(mockDocumentService.documentExists(notNull()))
        .willReturn(true);

    given()
      .auth().preemptive().basic("user", "password")
    .when()
      .delete("/api/v1/documents/{document}", D)
    .then()
      .log().all()
      .statusCode(204);
    // @formatter:on

    verify(mockDocumentService, times(1)).deleteDocument(D);
  }


  @Test
  public void test_getDocumentWithID() {
    // @formatter:off
    BDDMockito
      .given(mockDocumentService.getDocument(D))
        .willReturn(Optional.of(createTestDocumentWithContent()));

    // retrieve and verify GET document with ID
    RestAssured
      .given()
        .accept(ContentType.ANY)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{documentID}", D)
      .then()
        .log().all()
        .statusCode(200)
        .body("$", hasKey("documentId"))
        .body("$", hasKey("_links"))
        .body("_links", hasKey("self"))
        .body("$", hasKey("dateCreated"))
        .body("$", hasKey("dateModified"))
        .body("$", hasKey("contentElements"));
    // @formatter:on
  }

  @Test
  public void test_getDocumentWithIDAndIdFacet() {
    // @formatter:off
    BDDMockito
      .given(mockDocumentService.getDocument(D))
        .willReturn(Optional.of(createTestDocumentWithContent()));

    RestAssured
      .given()
        .accept(ContentType.ANY)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{documentID}?facets=documentId", D)
      .then()
        .statusCode(200)
        .body("$", hasKey("documentId"))
        .body("$", hasKey("_links"))
        .body("$", not(hasKey("dateCreated")))
        .body("$", not(hasKey("dateModified")))
        .body("$", not(hasKey("contentElements")));
    // @formatter:on
  }

  @Test
  public void test_getDocumentWithIDAndDateCreatedFacet() {
    // @formatter:off
    BDDMockito
      .given(mockDocumentService.getDocument(D))
        .willReturn(Optional.of(createTestDocumentWithContent()));

    RestAssured
      .given()
        .accept(ContentType.ANY)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{documentID}?facets=dateCreated", D)
      .then()
        .statusCode(200)
        .body("$", hasKey("documentId"))
        .body("$", not(hasKey("_links")))
        .body("$", hasKey("dateCreated"))
        .body("$", not(hasKey("dateModified")))
        .body("$", not(hasKey("contentElements")));
    // @formatter:on
  }

  @Test
  public void test_getDocumentWithIDAndDateModifiedFacet() {
    // @formatter:off
    BDDMockito
      .given(mockDocumentService.getDocument(D))
        .willReturn(Optional.of(createTestDocumentWithContent()));

    RestAssured
      .given()
        .accept(ContentType.ANY)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{documentID}?facets=dateModified", D)
      .then()
        .statusCode(200)
        .body("$", hasKey("documentId"))
        .body("$", not(hasKey("_links")))
        .body("$", not(hasKey("dateCreated")))
        .body("$", hasKey("dateModified"))
        .body("$", not(hasKey("contentElements")));
    // @formatter:on
  }

  @Test
  public void test_getDocumentWithIDAndMultipleFacets() {
    // @formatter:off
    BDDMockito
      .given(mockDocumentService.getDocument(D))
        .willReturn(Optional.of(createTestDocumentWithContent()));

    RestAssured
      .given()
        .accept(ContentType.ANY)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{documentID}?facets=documentId,contentElements,dateCreated", D)
      .then()
        .statusCode(200)
        .body("$", hasKey("documentId"))
        .body("$", hasKey("_links"))
        .body("_links", hasKey("contentElements"))
        .body("_links", hasKey("self"))
        .body("$", hasKey("dateCreated"))
        .body("$", not(hasKey("dateModified")))
        .body("$", hasKey("contentElements"));
    // @formatter:on
  }


  @Test
  public void testThat_documentReturnedFromCreateContainsFacets() {
    // @formatter:off
    BDDMockito
      .given(mockDocumentService.createDocument(notNull()))
        .willAnswer(i -> i.getArgument(0));
  
    // Create a document
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
        .body("{}")
      .when()
        .post("/api/v1/documents")
      .then()
        .statusCode(200)
        .body("$", hasKey("documentId"))
        .body("$", hasKey("_links"))
        .body("_links", hasKey("self"))
        .body("$", hasKey("dateCreated"))
        .body("$", hasKey("dateModified"))
        .body("$", not(hasKey("contentElements")));
    // @formatter:on
  }


  @Test
  public void testThat_documentReturnedFromCreateDoesNotContainExcludedFacets() {
    // @formatter:off
    BDDMockito
      .given(mockDocumentService.createDocument(notNull()))
        .willAnswer(i -> i.getArgument(0));
  
    // Create a document, specify only some facets to be returned
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
        .body("{}")
      .when()
        .post("/api/v1/documents?facets=documentId,contentElements,dateCreated")
      .then()
        .statusCode(200)
        .body("$", hasKey("documentId"))
        .body("$", hasKey("_links"))
        .body("_links", hasKey("self"))
        .body("$", hasKey("dateCreated"))
        .body("$", not(hasKey("dateModified")))
        .body("$", not(hasKey("contentElements")));
    // @formatter:on
  }


  @Test
  public void testThat_documentReturnedFromCreateWithIdContainsFacets() {
    // @formatter:off
    BDDMockito
      .given(mockDocumentService.getDocument(notNull()))
        .willReturn(Optional.of(createTestDocumentWithContent()));
    BDDMockito
      .given(mockDocumentService.update(notNull()))
        .willReturn(Optional.of(createTestDocumentWithContent()));
    BDDMockito
      .given(mockDocumentService.documentExists(notNull()))
        .willReturn(true);
  
    // retrieve and verify POST multipart documents
    RestAssured
      .given()
      .accept(ContentType.JSON)
      .contentType(ContentType.JSON)
      .auth().preemptive().basic("user", "password")
      .body("{}")
    .when()
      .log().all()
      .put("/api/v1/documents/{documentID}", D)
    .then()
      .log().all()
      .statusCode(200)
      .body("$", hasKey("documentId"))
      .body("$", hasKey("_links"))
      .body("_links", hasKey("self"))
      .body("$", hasKey("dateCreated"))
      .body("$", hasKey("dateModified"))
      .body("$", hasKey("contentElements"));
    // @formatter:on
  }

  @Test
  public void testThat_documentReturnedFromCreateWithIdDoesNotContainExcludedFacets() {
    // @formatter:off
    BDDMockito
      .given(mockDocumentService.getDocument(notNull()))
        .willReturn(Optional.of(createTestDocumentWithContent()));
    BDDMockito
      .given(mockDocumentService.update(notNull()))
        .willReturn(Optional.of(createTestDocumentWithContent()));
    BDDMockito
      .given(mockDocumentService.documentExists(notNull()))
        .willReturn(true);
  
    RestAssured
      .given()
      .accept(ContentType.JSON)
      .contentType(ContentType.JSON)
      .auth().preemptive().basic("user", "password")
      .body("{}")
    .when()
      .log().all()
      .put("/api/v1/documents/{documentID}?facets=documentId,contentElements,dateCreated", D)
    .then()
      .log().all()
      .statusCode(200)
      .body("$", hasKey("documentId"))
      .body("$", hasKey("_links"))
      .body("_links", hasKey("self"))
      .body("$", hasKey("dateCreated"))
      .body("$", not(hasKey("dateModified")))
      .body("$", hasKey("contentElements"));
    // @formatter:on
  }
}
