package com.neverpile.eureka.plugin.metadata.it;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import com.neverpile.authorization.api.CoreActions;
import com.neverpile.authorization.policy.AccessPolicy;
import com.neverpile.authorization.policy.AccessRule;
import com.neverpile.authorization.policy.Effect;
import com.neverpile.authorization.policy.PolicyRepository;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.model.HashAlgorithm;
import com.neverpile.eureka.plugin.metadata.rest.BaseTestConfiguration;
import com.neverpile.eureka.plugin.metadata.rest.MetadataDto;
import com.neverpile.eureka.plugin.metadata.rest.MetadataElementDto;
import com.neverpile.eureka.plugin.metadata.service.Metadata;
import com.neverpile.eureka.plugin.metadata.service.MetadataService;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
    BaseTestConfiguration.class
})
public class MetadataAuthorizationIntegrationTest extends AbstractRestAssuredTest {
  // Must mock the MultiVersioningDocumentService or we will break the app context initialization
  @MockBean
  MultiVersioningDocumentService mockDocumentService;

  @MockBean
  MetadataService mockMetadataService;

  @MockBean
  EventPublisher eventPublisher;

  @Autowired
  MockObjectStoreService mockObjectStoreService;

  @MockBean
  Authentication mockAuthentication;

  @MockBean
  PolicyRepository policyRepository;

  @Before
  public void initMock() {
    // provide dummy document
    given(mockDocumentService.getDocument(any())).willAnswer(
        i -> Optional.of(createTestDocumentWithContent(i.getArgument(0))));
    given(mockDocumentService.documentExists(any())).willReturn(true);
    given(mockMetadataService.get(any())).willAnswer(
        i -> Optional.of(modelMapper.map(createTestMetadata(), Metadata.class)));

    // simulate authenticated user
    given(mockAuthentication.isAuthenticated()).willReturn(true);
    given(mockAuthentication.getName()).willReturn("johndoe");

  }

  protected Document createTestDocument() {
    Document doc = new Document();

    doc.setDateCreated(new Date());
    doc.setDateModified(new Date());

    return doc;
  }

  @Test
  public void testThat_getDocumentAndMetadataAllowed() {
    // @formatter:off
    given(policyRepository.getCurrentPolicy()).will(i -> {
      return new AccessPolicy()
        .withDefaultEffect(Effect.DENY)
        .withRule(new AccessRule()
          .withName("Allow access to documents for authenticated users")
          .withEffect(Effect.ALLOW)
          .withSubjects("authenticated")
          .withActions(CoreActions.GET)
          .withResources("document")
        );
    });

    RestAssured.given()
        .accept(ContentType.ANY)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{documentID}", D)
      .then()
        .log().all()
        .statusCode(200)
        .body("documentId", equalTo(D))
        .body("metadata", hasKey("someJson"))
        .body("metadata", hasKey("someXml"));
    // @formatter:on
  }

  @Test
  public void testThat_getDocumentAndMetadataDeniedForAnonymous() {
    // @formatter:off
    given(policyRepository.getCurrentPolicy()).will(i -> {
      return new AccessPolicy()
          .withDefaultEffect(Effect.DENY)
          .withRule(new AccessRule()
            .withName("Allow access to documents for authenticated users")
            .withEffect(Effect.ALLOW)
            .withSubjects("authenticated")
            .withActions(CoreActions.GET)
            .withResources("document")
          );
    });
    
    RestAssured.given()
        .accept(ContentType.ANY) // not authenticated
      .when()
        .get("/api/v1/documents/{documentID}", D)
      .then()
        .log().all()
        .statusCode(HttpStatus.UNAUTHORIZED.value());
    // @formatter:on
  }

  @Test
  public void testThat_denyGetForSingleMetadataElement() {
    // @formatter:off
    given(policyRepository.getCurrentPolicy()).will(i -> {
      return new AccessPolicy()
        .withDefaultEffect(Effect.DENY)
        .withRule(new AccessRule()
          .withName("Deny access to xml metadata")
          .withEffect(Effect.DENY)
          .withSubjects("*")
          .withActions(CoreActions.GET)
          .withResources("document.metadata.someXml")
        )
        .withRule(new AccessRule()
          .withName("Allow access to documents for authenticated users")
          .withEffect(Effect.ALLOW)
          .withSubjects("authenticated")
          .withActions(CoreActions.GET)
          .withResources("document")
        );
    });

    RestAssured.given()
        .accept(ContentType.ANY)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/documents/{documentID}", D)
      .then()
        .log().all()
        .statusCode(200)
        .body("documentId", equalTo(D))
        .body("metadata", hasKey("someJson"))
        .body("metadata", not(hasKey("someXml")))
        ;
    // @formatter:on
  }

  private Document createTestDocumentWithContent(final String id) {
    Document doc = createTestDocument();

    doc.setDocumentId(id);

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

  private MetadataDto createTestMetadata() throws JsonProcessingException {
    // JSON
    MetadataElementDto jsonElement = new MetadataElementDto();
    jsonElement.setEncryption(EncryptionType.SHARED);
    jsonElement.setSchema("someJsonSchema");
    jsonElement.setDateCreated(new Date());
    jsonElement.setDateModified(new Date());

    ObjectNode metadataJson = objectMapper.createObjectNode() //
        .put("aString", "foo") //
        .put("aNumber", 1) //
        .put("aBoolean", true);

    metadataJson.putObject("anObject") //
        .put("bar", "baz");

    metadataJson.set("strings", objectMapper.createArrayNode().add("foo").add("bar").add("baz"));
    metadataJson.set("numbers", objectMapper.createArrayNode().add(42).add(4711).add(0x0815));

    jsonElement.setContent(objectMapper.writeValueAsBytes(metadataJson));
    jsonElement.setContentType(MediaType.APPLICATION_JSON_TYPE);

    // XML
    MetadataElementDto xmlElement = new MetadataElementDto();
    xmlElement.setDateCreated(new Date());
    xmlElement.setDateModified(new Date());
    xmlElement.setEncryption(EncryptionType.SHARED);
    xmlElement.setSchema("someXmlSchema");

    XMLTag xmlDocument = XMLDoc.newDocument(true).addRoot("aDocument") //
        .addAttribute("rootAttribute", "foo") //
        .addTag("aChild").addAttribute("childAttribute", "bar").addText("some text").gotoParent() //
        .addTag("anotherChild").addAttribute("childAttribute", "baz").addText("some more text").gotoRoot();

    xmlElement.setContent(xmlDocument.toString().getBytes(StandardCharsets.UTF_8));
    xmlElement.setContentType(MediaType.APPLICATION_XML_TYPE);

    // raw
    MetadataElementDto rawElement = new MetadataElementDto();
    rawElement.setDateCreated(new Date());
    rawElement.setDateModified(new Date());
    rawElement.setEncryption(EncryptionType.SHARED);
    rawElement.setSchema("someRawSchema");

    rawElement.setContent("foo".getBytes(StandardCharsets.UTF_8));
    rawElement.setContentType(MediaType.APPLICATION_OCTET_STREAM_TYPE);

    MetadataDto metadata = MetadataDto //
        .with("someJson", jsonElement) //
        .set("someXml", xmlElement) //
        .set("someRawBytes", rawElement);

    return metadata;
  }
}
