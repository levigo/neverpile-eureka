package com.neverpile.eureka.plugin.metadata.rest;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neverpile.authorization.api.AuthorizationService;
import com.neverpile.eureka.api.ContentElementService;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.plugin.metadata.service.Metadata;
import com.neverpile.eureka.plugin.metadata.service.MetadataElement;
import com.neverpile.eureka.plugin.metadata.service.MetadataService;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.content.ContentElementResource;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MetadataFacetTest extends AbstractRestAssuredTest {
  // Must mock the MultiVersioningDocumentService or we will break the app context initialization
  @MockBean
  MultiVersioningDocumentService mockDocumentService;

  @MockBean
  MetadataService mockMetadataService;

  @MockBean
  ContentElementService mockContentElementService;

  @Autowired
  MockObjectStoreService mockObjectStoreService;

  @MockBean
  DocumentIdGenerationStrategy idGenerationStrategy;

  @MockBean
  AuthorizationService authorizationService;

  @Before
  public void init() {
    when(idGenerationStrategy.validateDocumentId(D)).thenReturn(true);
    when(authorizationService.isAccessAllowed(any(), any(), any())).thenReturn(true);
  }

  @Test
  public void testThat_mappingOfFacetWorks() throws IOException {

    // prepare a document
    DocumentDto doc = new DocumentDto();

    doc.setDocumentId(D);

    MetadataElementDto element = new MetadataElementDto();
    ObjectNode metadataJson = JsonNodeFactory.instance.objectNode().put("foo", "bar").put("yada", 42);
    element.setContent(objectMapper.writeValueAsBytes(metadataJson));
    element.setContentType(MediaType.APPLICATION_JSON_TYPE);
    element.setSchema("mySchema");
    element.setEncryption(EncryptionType.SHARED);

    doc.setFacet("metadata", MetadataDto.with("foo", element));

    Date now = new Date();

    doc.setFacet("dateCreated", now);

    String serialized = objectMapper.writer().with(new DefaultPrettyPrinter()).writeValueAsString(doc);

    DocumentDto deserialized = objectMapper.readValue(serialized, DocumentDto.class);

    MetadataDto mdDto = (MetadataDto) deserialized.getFacets().get("metadata");
    assertThat(mdDto, notNullValue());
    assertThat(mdDto.getElements().keySet(), hasSize(1));

    MetadataElementDto md0 = mdDto.getElements().get("foo");
    assertThat(md0.getSchema(), equalTo("mySchema"));
    assertThat(md0.getContentType(), equalTo(MediaType.APPLICATION_JSON_TYPE));
    assertThat(md0.getEncryption(), equalTo(EncryptionType.SHARED));

    assertThat(deserialized.getFacets().get("dateCreated"), instanceOf(Date.class));
    assertThat(deserialized.getFacets().get("dateCreated"), equalTo(now));

    assertThat(md0.getContent(), equalTo(element.getContent()));
  }

  @Test
  public void testThat_documentCanBeCreated() throws Exception {
    Date then = new Date();

    // prepare a document
    DocumentDto doc = new DocumentDto();

    doc.setDocumentId(D);

    MetadataElement metadata1 = new MetadataElement();
    ObjectNode payload = JsonNodeFactory.instance.objectNode().put("foo", "bar").put("yada", 42);
    metadata1.setContent(objectMapper.writeValueAsBytes(payload));
    metadata1.setContentType(MediaType.APPLICATION_JSON_TYPE);
    metadata1.setSchema("mySchema");
    metadata1.setEncryption(EncryptionType.SHARED);

    Metadata metadata = new Metadata();
    metadata.put("foo", metadata1);

    MetadataDto metadataDto = modelMapper.map(metadata, MetadataDto.class);

    doc.setFacet("metadata", metadataDto);

    // @formatter:off
    // store it
    ArgumentCaptor<Document> storedDocumentC = ArgumentCaptor.forClass(Document.class);
    ArgumentCaptor<Metadata> metadataC = ArgumentCaptor.forClass(Metadata.class);
    
    BDDMockito
      .given(mockDocumentService.createDocument(storedDocumentC.capture()))
      .willAnswer(i -> i.getArgument(0));
    BDDMockito
      .given(mockMetadataService.store(any(), metadataC.capture()))
        .willReturn(metadata);
    BDDMockito
      .given(mockMetadataService.get(any()))
        .willReturn(Optional.of(metadata));
    
    DocumentDto returnedDocument = RestAssured
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

    // verify returned document
    Date now = new Date();
    assertThat(returnedDocument.getDocumentId(), equalTo(D));
    assertThat(((MetadataDto)returnedDocument.getFacets().get("metadata")), equalTo(metadataDto));
    
    // verify stored document
    Document storedDocument = storedDocumentC.getValue();
    assertThat(storedDocument.getDocumentId(), equalTo(D));
    assertThat(storedDocument.getDateCreated(), allOf(greaterThanOrEqualTo(then), lessThanOrEqualTo(now)));
    
    assertThat(metadataC.getValue(), equalTo(metadata));
    
    MetadataElement stored = metadataC.getValue().get("foo");
    
    assertThat(stored, equalTo(metadata1));
    // @formatter:on
  }
}
