package com.neverpile.eureka.plugin.audit.rest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.impl.contentservice.SimpleContentElementService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.service.AuditEvent.Type;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.content.ContentElementFacet;
import com.neverpile.eureka.rest.api.document.content.ContentElementResource;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuditFacetTest extends AbstractRestAssuredTest {
  @TestConfiguration
  @Import({AuditLogFacet.class, AuditLogResource.class, SimpleContentElementService.class, ContentElementFacet.class,
      ContentElementResource.class
  })
  public static class ServiceConfig {
  }

  // Must mock the MultiVersioningDocumentService or we will break the app context initialization
  @MockBean
  MultiVersioningDocumentService mockDocumentService;

  @MockBean
  SimpleContentElementService mockContentElementService;


  @Autowired
  MockObjectStoreService mockObjectStoreService;

  @MockBean
  AuditLogService mockAuditLogService;

  @MockBean
  DocumentIdGenerationStrategy idGenerationStrategy;

  @Before
  public void init() {
    when(idGenerationStrategy.validateDocumentId(D)).thenReturn(true);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testThat_mappingOfFacetWorks() throws IOException {
    // prepare a document
    DocumentDto doc = new DocumentDto();

    doc.setDocumentId(D);

    AuditEvent event = new AuditEvent();
    event.setTimestamp(Instant.now());
    event.setUserID("HarryHirsch");
    event.setDescription("Unit Test");
    event.setType(Type.CREATE);

    doc.setFacet("audit", Collections.singletonList(event));

    String serialized = objectMapper.writer().with(new DefaultPrettyPrinter()).writeValueAsString(doc);

    DocumentDto deserialized = objectMapper.readValue(serialized, DocumentDto.class);

    List<AuditEventDto> alDtos = (List<AuditEventDto>) deserialized.getFacets().get("audit");
    assertThat(alDtos, notNullValue());
    assertThat(alDtos, hasSize(1));
    AuditEventDto ae0 = alDtos.get(0);
    assertThat(ae0.getTimestamp(), equalTo(event.getTimestamp()));
    assertThat(ae0.getUserID(), equalTo("HarryHirsch"));
  }

  @Test
  public void testThat_documentCanBeCreated() throws Exception {
    // prepare a document
    DocumentDto doc = new DocumentDto();

    doc.setDocumentId(D);

    // @formatter:off
    // store it
    ArgumentCaptor<Document> storedDocumentC = ArgumentCaptor.forClass(Document.class);
    ArgumentCaptor<AuditEvent> eventC = ArgumentCaptor.forClass(AuditEvent.class);
    
    BDDMockito
      .given(mockDocumentService.createDocument(storedDocumentC.capture()))
      .willAnswer(i -> i.getArgument(0));
    Mockito.doNothing().when(mockAuditLogService).logEvent(eventC.capture());
    
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .multiPart(ContentElementResource.DOCUMENT_FORM_ELEMENT_NAME, doc, ContentType.JSON.toString())
        .auth().preemptive().basic("user", "password")
      .when()
        .log().all()
        .post("/api/v1/documents")
      .then()
        .log().all()
        .statusCode(201)
        .contentType(ContentType.JSON)
        .extract().as(DocumentDto.class);

    assertThat(eventC.getValue().getType(), equalTo(Type.CREATE));
    // @formatter:on
  }

  @Test
  public void testThat_retrievedDocumentContainsAuditLog() throws Exception {
    AuditEvent event = new AuditEvent();
    event.setTimestamp(Instant.ofEpochMilli(12345678L));
    event.setUserID("HarryHirsch");
    event.setDescription("Unit Test");
    event.setType(Type.CREATE);

    Document doc = new Document();
    doc.setDocumentId(D);

    // @formatter:off
    // retrieve it
    BDDMockito
      .given(mockDocumentService.getDocument(D))
        .willReturn(Optional.of(doc));
    BDDMockito
      .given(mockAuditLogService.getEventLog(D))
        .willReturn(Collections.singletonList(event));

    RestAssured
      .given()
        .accept(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
      .when()
        .queryParam("facets", "audit")
        .get("/api/v1/documents/{documentID}", D)
      .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("audit.size()", equalTo(1))
        .body("audit[0].timestamp", equalTo("1970-01-01T03:25:45.678+0000"))
        .body("audit[0].userID", equalTo("HarryHirsch"))
        .body("audit[0].description", equalTo("Unit Test"))
        .body("audit[0].type", equalTo("CREATE"))
        .extract().as(DocumentDto.class);
    // @formatter:on
  }

  @Test
  public void testThat_auditLogCanBeRetrieved() throws Exception {
    AuditEvent event = new AuditEvent();
    event.setTimestamp(Instant.ofEpochMilli(12345678L));
    event.setUserID("HarryHirsch");
    event.setDescription("Unit Test");
    event.setType(Type.CREATE);

    Document doc = new Document();
    doc.setDocumentId(D);

    // @formatter:off
    // retrieve it
    given(mockDocumentService.getDocument(D))
      .willReturn(Optional.of(doc));
    given(mockAuditLogService.getEventLog(D))
      .willReturn(Collections.singletonList(event));

    RestAssured
      .given()
        .accept(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{documentID}/audit", D)
      .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("size()", equalTo(1))
        .body("[0].timestamp", equalTo("1970-01-01T03:25:45.678+0000"))
        .body("[0].userID", equalTo("HarryHirsch"))
        .body("[0].description", equalTo("Unit Test"))
        .body("[0].type", equalTo("CREATE"));
    // @formatter:on
  }

  @Test
  public void testThat_auditLogCanBeAppendedTo() throws Exception {
    AuditEvent event = new AuditEvent();
    event.setTimestamp(Instant.ofEpochMilli(12345979L));
    event.setUserID("replaced by principal!");
    event.setDescription("Unit Test");
    event.setType(Type.UPDATE);

    Document doc = new Document();
    doc.setDocumentId(D);

    // @formatter:off
    // retrieve it
    BDDMockito
      .given(mockDocumentService.getDocument(D))
        .willReturn(Optional.of(doc));
    BDDMockito
      .given(mockAuditLogService.getEventLog(D))
        .willReturn(Collections.singletonList(event));
    
    ArgumentCaptor<AuditEvent> eventC = ArgumentCaptor.forClass(AuditEvent.class);
    Mockito.doNothing().when(mockAuditLogService).logEvent(eventC.capture());
    
    RestAssured
      .given()
        .contentType(ContentType.JSON)
        .accept(ContentType.JSON)
        .body(event)
        .auth().preemptive().basic("user", "password")
      .when()
        .post("/api/v1/documents/{documentID}/audit", D)
      .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("timestamp", equalTo("1970-01-01T03:25:45.979+0000"))
        .body("userID", equalTo("user"))
        .body("description", equalTo("Unit Test"))
        .body("type", equalTo("UPDATE"));
    // @formatter:on

    // verify logged event
    AuditEvent appended = eventC.getValue();
    assertThat(appended.getTimestamp(), equalTo(event.getTimestamp()));
    assertThat(appended.getUserID(), equalTo("user"));
    assertThat(appended.getDescription(), equalTo(event.getDescription()));
    assertThat(appended.getType(), equalTo(event.getType()));
  }

  @Test
  public void testThat_onlySelectedFacetIsReturned() throws Exception {
    AuditEvent event = new AuditEvent();
    event.setTimestamp(Instant.ofEpochMilli(12345678L));
    event.setUserID("HarryHirsch");
    event.setDescription("Unit Test");
    event.setType(Type.CREATE);

    // @formatter:off
    // retrieve it
    Document doc = new Document();
    doc.setDocumentId(D);
    BDDMockito
      .given(mockDocumentService.getDocument(D))
        .willReturn(Optional.of(doc));
    BDDMockito
      .given(mockAuditLogService.getEventLog(D))
        .willReturn(Collections.singletonList(event));

    RestAssured
      .given()
        .accept(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{documentID}?facets=doesNotExist", D)
      .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("auditLog", nullValue());
    
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{documentID}?facets=audit", D)
      .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("audit", notNullValue());
    // @formatter:on
  }
}
