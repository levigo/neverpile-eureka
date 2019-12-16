package com.neverpile.eureka.plugin.metadata.rest;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLTag;
import com.neverpile.authorization.api.AuthorizationContext;
import com.neverpile.authorization.api.AuthorizationService;
import com.neverpile.authorization.api.CoreActions;
import com.neverpile.eureka.api.DocumentAuthorizationService;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.plugin.metadata.service.Metadata;
import com.neverpile.eureka.plugin.metadata.service.MetadataService;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MetadataAuthContextTest extends AbstractRestAssuredTest {
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
  AuthorizationService authService;

  @Autowired
  DocumentAuthorizationService documentAuthorizationService;

  @Before
  public void initMock() {
    // provide dummy document
    given(mockDocumentService.getDocument(any())).willAnswer(i -> Optional.of(new Document(i.getArgument(0))));
    given(mockDocumentService.documentExists(any())).willReturn(true);

    // allow create
    given(mockMetadataService.get(any())).willAnswer(
        i -> Optional.of(modelMapper.map(createTestMetadata(), Metadata.class)));
  }

  @Test
  public void testThat_resourcePathIsCorrect() throws Exception {
    given(authService.isAccessAllowed(any(), any(), any())).willReturn(true);

    assertThat(documentAuthorizationService.authorizeSubresourceGet(new Document(D), "metadata")).isTrue();

    verify(authService).isAccessAllowed(eq("document.metadata"), eq(singleton(CoreActions.GET)), any());
  }

  @Test
  public void testThat_authContextValuesAreCorrect() throws Exception {
    ArgumentCaptor<AuthorizationContext> authContextC = ArgumentCaptor.forClass(AuthorizationContext.class);
    given(authService.isAccessAllowed(any(), any(), authContextC.capture())).willReturn(true);

    documentAuthorizationService.authorizeSubresourceGet(new Document(D), "metadata");

    AuthorizationContext authContext = authContextC.getValue();

    assertThat(authContext.resolveValue("document.metadata")).isEqualTo(Boolean.TRUE);
    assertThat(authContext.resolveValue("document.somethingelse")).isNull();

    assertThat(authContext.resolveValue("document.metadata.someNonExistingElement")).isEqualTo(Boolean.FALSE);

    assertThat(authContext.resolveValue("document.metadata.someJson")).isEqualTo(Boolean.TRUE);

    assertThat((Instant) authContext.resolveValue("document.metadata.someJson.dateCreated")).isCloseTo(Instant.now(),
        within(500, ChronoUnit.MILLIS));
    assertThat((Instant) authContext.resolveValue("document.metadata.someJson.dateModified")).isCloseTo(Instant.now(),
        within(500, ChronoUnit.MILLIS));
    assertThat((String) authContext.resolveValue("document.metadata.someJson.encryption")).isEqualTo("SHARED");
    assertThat((String) authContext.resolveValue("document.metadata.someJson.schema")).isEqualTo("someJsonSchema");

    assertThat(authContext.resolveValue("document.metadata.someJson.json.aString")).isEqualTo("foo");
    assertThat(authContext.resolveValue("document.metadata.someJson.json.aNumber")).isEqualTo(1);
    assertThat(authContext.resolveValue("document.metadata.someJson.json.aBoolean")).isEqualTo(true);
    assertThat(authContext.resolveValue("document.metadata.someJson.json.anObject.bar")).isEqualTo("baz");
    assertThat(authContext.resolveValue("document.metadata.someJson.json.strings[0]")).isEqualTo("foo");
    assertThat(authContext.resolveValue("document.metadata.someJson.json.strings[1]")).isEqualTo("bar");
    assertThat(authContext.resolveValue("document.metadata.someJson.json.numbers[0]")).isEqualTo(42);
    assertThat(authContext.resolveValue("document.metadata.someJson.json.numbers[1]")).isEqualTo(4711);

    assertThat(authContext.resolveValue("document.metadata.someXml")).isEqualTo(Boolean.TRUE);

    assertThat(authContext.resolveValue("document.metadata.someXml.xml./aDocument/@rootAttribute")).isEqualTo("foo");
    assertThat(authContext.resolveValue("document.metadata.someXml.xml./aDocument/aChild/@childAttribute")).isEqualTo(
        "bar");
    assertThat(authContext.resolveValue("document.metadata.someXml.xml./aDocument/aChild")).isEqualTo("some text");
    assertThat(authContext.resolveValue("document.metadata.someXml.xml./aDocument/anotherChild")).isEqualTo(
        "some more text");

    assertThat(authContext.resolveValue("document.metadata.someRawBytes")).isEqualTo(Boolean.TRUE);
  }

  private MetadataDto createTestMetadata() throws JsonProcessingException {
    // JSON
    MetadataElementDto jsonElement = new MetadataElementDto();
    jsonElement.setEncryption(EncryptionType.SHARED);
    jsonElement.setSchema("someJsonSchema");
    jsonElement.setDateCreated(Instant.now());
    jsonElement.setDateModified(Instant.now());

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
    xmlElement.setDateCreated(Instant.now());
    xmlElement.setDateModified(Instant.now());
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
    rawElement.setDateCreated(Instant.now());
    rawElement.setDateModified(Instant.now());
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
