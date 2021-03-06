package com.neverpile.eureka.api.documentservice;

import static com.neverpile.eureka.api.ObjectStoreService.NEW_VERSION;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neverpile.eureka.api.DocumentAssociatedEntityStore;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.api.DocumentService.DocumentAlreadyExistsException;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.impl.documentservice.DefaultDocumentService;
import com.neverpile.eureka.impl.documentservice.DocumentPdo;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.ObjectName;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultDocumentServiceTest {

  protected static final String D = "aDocument";

  @TestConfiguration
  @EnableTransactionManagement
  @EnableAutoConfiguration
  public static class ServiceConfig {

    @Bean
    DefaultDocumentService documentService() {
      return new DefaultDocumentService();
    }

  }

  @MockBean
  protected ObjectStoreService objectStoreService;

  @MockBean
  protected EventPublisher eventPublisher;

  @Autowired
  protected ObjectMapper mapper;

  @Autowired
  protected DocumentService documentService;

  @Autowired
  protected DocumentAssociatedEntityStore entityStore;

  @Autowired
  protected TransactionTemplate transactionTemplate;

  @Test
  public void testThat_documentCanBeCreated() throws Exception {
    given(objectStoreService.get(any())).willReturn(null);
    Document doc = new Document();
    doc.setDocumentId("aDocument");
    transactionTemplate.execute(status -> documentService.createDocument(doc));
    ArgumentCaptor<InputStream> isC = ArgumentCaptor.forClass(InputStream.class);
    verify(objectStoreService) //
        .put(eq(ObjectName.of("document", "aDocument", "document.json")), eq(ObjectStoreService.NEW_VERSION),
            isC.capture(), anyLong());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy(isC.getValue(), baos);
    Document readBack = mapper.readValue(new ByteArrayInputStream(baos.toByteArray()), Document.class);
    assertThat(readBack.getDocumentId(), equalTo("aDocument"));
  }

  @Test(
      expected = DocumentAlreadyExistsException.class)
  public void testThat_documentCantBeCreatedTwice() throws Exception {
    Document doc = new Document();
    doc.setDocumentId("aDocument");
    ObjectName name = ObjectName.of("document", "aDocument", "document.json");
    given(objectStoreService.get(eq(name))).will(i -> new DocObject(mapper, doc, name));
    documentService.createDocument(doc);
  }

  @Test
  public void testThat_documentCanBeFound() throws IOException {
    Document doc = new Document();
    doc.setDocumentId("aDocument");
    ObjectName name = ObjectName.of("document", "aDocument", "document.json");
    given(objectStoreService.get(eq(name))).will(i -> new DocObject(mapper, doc, name));
    Document aDocument = documentService.getDocument("aDocument").get();
    assertThat(aDocument.getDocumentId(), equalTo("aDocument"));
    verify(objectStoreService).get(name);
  }

  @Test(
      expected = IllegalStateException.class)
  public void testThat_mutationsRequireATransaction() throws Exception {
    Document doc = new Document();
    doc.setDocumentId("aDocument");
    documentService.createDocument(doc);
  }

  protected ArgumentCaptor<InputStream> verifyPersistOnce(final String expectedVersion) {
    ArgumentCaptor<InputStream> isC = ArgumentCaptor.forClass(InputStream.class);
    verify(objectStoreService).put(any(), eq(expectedVersion), isC.capture(), anyLong());
    verify(objectStoreService, atLeastOnce()).get(any());
    verifyNoMoreInteractions(objectStoreService);
    return isC;
  }

  @Test
  public void testThat_existingDocumentDataCanBeRetrieved() throws Exception {
    DocumentPdo existing = prepareEmptyDocument();
    ObjectNode something = mapper.createObjectNode().put("some", "thing");
    existing.putSidecarElement("existing", something);
    given(objectStoreService.get(any())).will(i -> new DocObject(mapper, existing, i.getArgument(0)));
    assertThat(entityStore.retrieve(existing, "existing").get(), equalTo(something));
  }

  @Test
  public void testThat_documentDataCanBeAssociatedBeforeCreate() throws Exception {
    given(objectStoreService.get(any())).willReturn(null);
    transactionTemplate.execute(status -> {
      Document doc = prepareEmptyDocument();
      entityStore.store(doc, "foo", mapper.createObjectNode().put("bar", "baz"));
      return documentService.createDocument(doc);
    });
    assertSchemaF(verifyPersistOnce(NEW_VERSION));
  }

  @Test
  public void testThat_documentDataCanBeAssociatedAfterCreate() throws Exception {
    given(objectStoreService.get(any())).willReturn(null);
    transactionTemplate.execute(status -> {
      Document doc = prepareEmptyDocument();
      documentService.createDocument(doc);
      entityStore.store(doc, "foo", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });
    assertSchemaF(verifyPersistOnce(NEW_VERSION));
  }

  @Test
  public void testThat_documentDataCanBeAssociatedUponUpdate() throws Exception {
    given(objectStoreService.get(any())).will(i -> new DocObject(mapper, prepareEmptyDocument(), i.getArgument(0)));
    Instant now = Instant.now();
    transactionTemplate.execute(status -> {
      Document toBeUpdated = documentService.getDocument(D).get();
      toBeUpdated.setDateModified(now);
      documentService.update(toBeUpdated);
      entityStore.store(toBeUpdated, "foo", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });
    Document doc = assertSchemaF(verifyPersistOnce("0"));
    assertThat(doc.getDateModified(), equalTo(now));
  }

  @Test
  public void testThat_documentDataCanBeAssociatedWithExistingDocument() throws Exception {
    given(objectStoreService.get(any())).will(i -> new DocObject(mapper, prepareEmptyDocument(), i.getArgument(0)));
    Document existing = prepareEmptyDocument();
    transactionTemplate.execute(status -> {
      entityStore.store(existing, "foo", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });
    assertSchemaF(verifyPersistOnce("0"));
  }

  @Test
  public void testThat_multipleAddsWork() throws Exception {
    given(objectStoreService.get(any())).will(i -> new DocObject(mapper, prepareEmptyDocument(), i.getArgument(0)));
    Document existing = prepareEmptyDocument();
    transactionTemplate.execute(status -> {
      entityStore.store(existing, "foo", mapper.createObjectNode().put("bar", "baz"));
      entityStore.store(existing, "bar", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });
    DocumentPdo doc = assertSchemaF(verifyPersistOnce("0"));
    assertThat(doc.getSidecarElement("bar"), equalTo(mapper.createObjectNode().put("bar", "baz")));
  }

  @Test
  public void testThat_documentDataCanBeRemoved() throws Exception {
    DocumentPdo existing = prepareEmptyDocument();
    existing.putSidecarElement("existing", mapper.createObjectNode().put("some", "thing"));
    given(objectStoreService.get(any())).will(i -> new DocObject(mapper, existing, i.getArgument(0)));
    transactionTemplate.execute(status -> {
      entityStore.delete(existing, "existing");
      return null;
    });
    Document doc = getCapturedDocument(verifyPersistOnce("0"));
    assertThat(doc.getDocumentId(), equalTo(D));
  }

  protected DocumentPdo prepareEmptyDocument() {
    DocumentPdo doc = new DocumentPdo(D);
    doc.setVersionTimestamp(Instant.now());
    return doc;
  }

  @Test
  public void testThat_addAndRemoveCanBeCombined() throws Exception {
    DocumentPdo existing = prepareEmptyDocument();
    existing.putSidecarElement("existing", mapper.createObjectNode().put("some", "thing"));
    given(objectStoreService.get(any())).will(i -> new DocObject(mapper, existing, i.getArgument(0)));
    transactionTemplate.execute(status -> {
      entityStore.delete(existing, "existing");
      entityStore.store(existing, "foo", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });
    assertSchemaF(verifyPersistOnce("0"));
  }

  @Test
  public void testThat_existingAssociatedDataIsPreserved() throws Exception {
    DocumentPdo existing = prepareEmptyDocument();
    existing.putSidecarElement("existing", mapper.createObjectNode().put("some", "thing"));
    given(objectStoreService.get(any())).will(i -> new DocObject(mapper, existing, i.getArgument(0)));
    transactionTemplate.execute(status -> {
      entityStore.store(existing, "foo", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });
    DocumentPdo doc = assertSchemaF(verifyPersistOnce("0"));
    assertThat(doc.getSidecarElement("existing"), equalTo(mapper.createObjectNode().put("some", "thing")));
  }

  @Test
  public void testThat_nothingIsPersistedUponRollback() throws Exception {
    transactionTemplate.execute(status -> {
      Document existing = prepareEmptyDocument();
      entityStore.store(existing, "foo", mapper.createObjectNode().put("bar", "baz"));
      status.setRollbackOnly();
      return null;
    });
    verify(objectStoreService).get(any());
    verifyNoMoreInteractions(objectStoreService);
  }

  /**
   * Assert that the persisted document conforms to the state expected by most tests.
   *
   * @param isC
   * @return
   * @throws IOException
   * @throws JsonParseException
   * @throws JsonMappingException
   */
  private DocumentPdo assertSchemaF(final ArgumentCaptor<InputStream> isC)
      throws IOException, JsonParseException, JsonMappingException {
    DocumentPdo doc = getCapturedDocument(isC);
    assertThat(doc.getDocumentId(), equalTo(D));
    assertThat(doc.getSidecarElement("foo"), equalTo(mapper.createObjectNode().put("bar", "baz")));
    return doc;
  }

  private DocumentPdo getCapturedDocument(final ArgumentCaptor<InputStream> isC)
      throws IOException, JsonParseException, JsonMappingException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy(isC.getValue(), baos);
    return mapper.readValue(new ByteArrayInputStream(baos.toByteArray()), DocumentPdo.class);
  }

}
