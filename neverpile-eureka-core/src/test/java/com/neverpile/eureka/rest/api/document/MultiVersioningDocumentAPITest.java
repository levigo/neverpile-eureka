package com.neverpile.eureka.rest.api.document;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.neverpile.eureka.api.BaseTestConfiguration;
import com.neverpile.eureka.api.ContentElementIdGenerationStrategy;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.impl.contentservice.SimpleContentElementService;
import com.neverpile.eureka.impl.documentservice.DefaultMultiVersioningDocumentService;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.model.HashAlgorithm;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BaseTestConfiguration.class)
public class MultiVersioningDocumentAPITest extends AbstractRestAssuredTest {
//  @TestConfiguration
//  @Import({
//      ModelMapperConfiguration.class, SimpleContentElementService.class, ContentElementFacet.class,
//      MultiVersioningDocumentResource.class
//  })
//  public static class ServiceConfig {
//  }

  @MockitoBean
  MultiVersioningDocumentService mockDocumentService;

  @MockitoBean
  EventPublisher eventPublisher;

  @Autowired
  SimpleContentElementService contentElementService;

  @Autowired
  MockObjectStoreService mockObjectStoreService;

  @MockitoBean
  DocumentIdGenerationStrategy documentIdGenerationStrategy;

  @MockitoBean
  ContentElementIdGenerationStrategy contentElementIdGenerationStrategy;

  @BeforeEach
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

  @Test
  public void testThat_documentVersionCanBeRetrievedAsJSON() throws Exception {
    Instant then = Instant.now();

    Document doc = createTestDocumentWithContent();
    Instant versionTimestamp = Instant.now();
    doc.setVersionTimestamp(versionTimestamp);

    // @formatter:off
    // retrieve it
    BDDMockito
      .given(mockDocumentService.getDocumentVersion(D, versionTimestamp))
        .willReturn(Optional.of(doc));

    Document returnedDocument = RestAssured
      .given()
        .accept(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
      .when()
        .log().all()
        .get("/api/v1/documents/{document}/history/{timestamp}", D, fmt(versionTimestamp))
      .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("documentId", equalTo(D))
        .body("versionTimestamp", equalTo(fmt(versionTimestamp)))
        .extract().as(Document.class);
    // @formatter:on

    // verify returned document
    Instant now = Instant.now();
    assertThat(returnedDocument.getDocumentId(), equalTo(D));
    assertThat(returnedDocument.getVersionTimestamp(), equalTo(versionTimestamp));
    assertThat(returnedDocument.getDateCreated(), allOf(greaterThanOrEqualTo(then), lessThanOrEqualTo(now)));
  }

  private String fmt(final Instant versionTimestamp) {
    return DefaultMultiVersioningDocumentService.VERSION_FORMATTER.format(versionTimestamp);
  }
  
  @Test
  public void testThat_documentVersionListCanBeRetrievedAsJSON() throws Exception {
    Instant v1 = Instant.ofEpochMilli(1234567L);
    Instant v2 = Instant.ofEpochMilli(2345678L);
    Instant v3 = Instant.ofEpochMilli(3456789L);
    
    // @formatter:off
    // retrieve it
    BDDMockito
      .given(mockDocumentService.getVersions(D))
        .willReturn(java.util.Arrays.asList(v1, v2, v3));

    RestAssured
      .given()
        .accept(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{document}/history", D)
      .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("size()", is(3))
        .body("[0]", equalTo(fmt(v1)))
        .body("[1]", equalTo(fmt(v2)))
        .body("[2]", equalTo(fmt(v3)));
    // @formatter:on
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
        .get("/api/v1/documents/{document}/history", "IDoNotExist.SorryBoutThat")
    .then()
        .log().all()
        .statusCode(404);
    // @formatter:on
  }

  @Test
  public void testThat_nonexistentDocumentVersionYields404() throws Exception {
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
    Document doc = createTestDocumentWithContent();
    Instant versionTimestamp = Instant.now();
    doc.setVersionTimestamp(versionTimestamp);

    // @formatter:off
    // retrieve it
    BDDMockito
      .given(mockDocumentService.getDocumentVersion(D, versionTimestamp))
        .willReturn(Optional.of(doc));
    
    given()
      .accept(ContentType.JSON)
    .when()
        .get("/api/v1/documents/{document}/history/{versionTimestamp}", D, Instant.ofEpochMilli(12345L))
    .then()
        .statusCode(401);
    // @formatter:on
  }
}
