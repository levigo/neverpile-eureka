package com.neverpile.eureka.rest.api.document.content;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {BaseTestConfiguration.class, MultiVersioningContentElementResource.class, DefaultMultiVersioningDocumentService.class})
public class MultiVersioningDocumentContentAPITest extends AbstractRestAssuredTest {
  // Must mock the MultiVersioningDocumentService or we will break the app context initialization
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
  public void testThat_contentQueryWorksAgainstHistory() throws Exception {
    Document doc = createTestDocumentWithContent();

    // @formatter:off
    given(mockDocumentService.getDocumentVersion(D, doc.getVersionTimestamp()))
      .willReturn(Optional.of(doc));

    // retrieve and verify parts
    RestAssured
        .given()
          .log().all()
          .accept(ContentType.ANY)
          .auth().preemptive().basic("user", "password")
        .when()
          .queryParam("role", "part")
          .queryParam("return", "first")
          .get("/api/v1/documents/{documentID}/history/1970-01-01T00:00:00.042Z/content", D)
        .then()
          .log().all()
          .statusCode(200)
          .contentType("text/plain")
          .header("Content-Disposition", Matchers.startsWith("inline; name=\"part\"; filename=\"foo.txt\""))
          .header("Digest", Matchers.equalTo("sha-256=LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564="))
          .header("ETag", Matchers.equalTo("\"sha-256_LCa0a2j/xo/5m0U8HTBBNBNCLXBkg7+g+YpeiGJm564=\""))
          .body(equalTo("foo"));
    
    verify(mockDocumentService).getDocumentVersion(D, doc.getVersionTimestamp());
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

  // @formatter:off

}
