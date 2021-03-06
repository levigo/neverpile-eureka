package com.neverpile.eureka.api.documentservice;

import static com.neverpile.eureka.impl.documentservice.DefaultMultiVersioningDocumentService.DOCUMENT_PREFIX;
import static com.neverpile.eureka.impl.documentservice.DefaultMultiVersioningDocumentService.VERSION_FORMATTER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neverpile.eureka.api.DocumentAssociatedEntityStore;
import com.neverpile.eureka.api.DocumentService.DocumentAlreadyExistsException;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.exception.VersionMismatchException;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.impl.documentservice.DefaultMultiVersioningDocumentService;
import com.neverpile.eureka.impl.documentservice.DocumentPdo;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.rest.api.exception.NotFoundException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultMultiVersioningDocumentServiceTest {
  private static final String D = "aDocument";

  @TestConfiguration
  @EnableTransactionManagement
  @EnableCaching
  public static class ServiceConfig {
    @Bean
    MultiVersioningDocumentService documentService() {
      return new DefaultMultiVersioningDocumentService();
    }
  }

  @MockBean
  protected ObjectStoreService objectStoreService;
  @MockBean
  protected EventPublisher eventPublisher;
  @Autowired
  protected ObjectMapper mapper;
  @Autowired
  protected MultiVersioningDocumentService documentService;
  @Autowired
  protected DocumentAssociatedEntityStore entityStore;
  @Autowired
  protected TransactionTemplate transactionTemplate;

  @Autowired
  CacheManager cacheManager;
  
  @Before
  public void clearCache() {
    // The cache isn't automatically cleared between test methods
    cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
  }
  
  @Test
  public void testThat_documentCanBeCreated() throws Exception {
    mockNonexistentDocument();

    Document doc = anEmptyDocument();

    long then = System.currentTimeMillis();

    Document persisted = transactionTemplate.execute(status -> documentService.createDocument(doc));

    ArgumentCaptor<InputStream> isC = ArgumentCaptor.forClass(InputStream.class);
    ArgumentCaptor<ObjectName> objectNameC = ArgumentCaptor.forClass(ObjectName.class);
    verify(objectStoreService) //
        .put(objectNameC.capture(), eq(ObjectStoreService.NEW_VERSION), isC.capture(), anyLong());

    Instant timestampFromObjectName = Instant.from(VERSION_FORMATTER.parse(objectNameC.getValue().tail()));

    assertThat(ObjectName.of(DOCUMENT_PREFIX, D, "meta").isPrefixOf(objectNameC.getValue()), is(true));

    assertThat(persisted.getDocumentId(), equalTo(D));
    assertThat(persisted.getVersionTimestamp(), equalTo(timestampFromObjectName));

    Document readBack = readBackFromStream(isC);
    assertThat(readBack.getDocumentId(), equalTo(D));
    assertThat(readBack.getVersionTimestamp(), equalTo(timestampFromObjectName));

    long now = System.currentTimeMillis();

    assertThat(readBack.getVersionTimestamp().toEpochMilli(),
        allOf(greaterThanOrEqualTo(then), lessThanOrEqualTo(now)));
  }

  private DocumentPdo readBackFromStream(final ArgumentCaptor<InputStream> isC)
      throws IOException, JsonParseException, JsonMappingException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy(isC.getValue(), baos);

    return mapper.readValue(new ByteArrayInputStream(baos.toByteArray()), DocumentPdo.class);
  }

  @Test
  public void testThat_documentCanBeUpdatedWithVersionChecking() throws Exception {
    Document existing = prepareEmptyDocument();
    mockExistingVersion(existing);

    Document updated = transactionTemplate.execute(status -> {
      Document update = prepareEmptyDocument();

      // backend must check for correct timestamp
      update.setVersionTimestamp(existing.getVersionTimestamp());

      // associate some facet data
      entityStore.store(update, "foo", mapper.createObjectNode().put("bar", "baz"));

      update.setDateModified(Instant.now());
      return documentService.update(update).get();
    });

    // must be new version
    assertThat(updated.getVersionTimestamp(), greaterThan(existing.getVersionTimestamp()));

    // verify object store interaction
    DocumentPdo readBack = readBackFromStream(verifyStorePut());

    // verify stored document
    assertThat(readBack.getDocumentId(), equalTo(D));
    assertThat(readBack.getVersionTimestamp(), equalTo(updated.getVersionTimestamp()));
    assertThat(readBack.getSidecarElement("foo"), equalTo(mapper.createObjectNode().put("bar", "baz")));
  }

  private ArgumentCaptor<InputStream> verifyStorePut() {
    ArgumentCaptor<ObjectName> objectNameC = ArgumentCaptor.forClass(ObjectName.class);
    ArgumentCaptor<InputStream> isC = ArgumentCaptor.forClass(InputStream.class);
    verify(objectStoreService).put(objectNameC.capture(), eq(ObjectStoreService.NEW_VERSION), isC.capture(), anyLong());
    return isC;
  }

  @Test
  public void testThat_documentCanBeUpdatedWithNullVersionTimestamp() throws Exception {
    Document existing = prepareEmptyDocument();
    mockExistingVersion(existing);

    Document updated = transactionTemplate.execute(status -> {
      Document update = prepareEmptyDocument();

      // backend must generate new version timestamp
      update.setVersionTimestamp(null);
      return documentService.update(update).get();
    });

    // must be new version
    assertThat(updated.getVersionTimestamp(), greaterThan(existing.getVersionTimestamp()));
  }

  @Test(expected = VersionMismatchException.class)
  public void testThat_documentUpdateFailsWithIncorrectTimestamp() throws Exception {
    Document existing = prepareEmptyDocument();
    mockExistingVersion(existing);
    Thread.sleep(100);

    transactionTemplate.execute(status -> {
      // update has _wrong_ timestamp!
      Document update = prepareEmptyDocument();
      return documentService.update(update).get();
    });
  }

  @Test
  public void testThat_documentCanBeUpdateMultipleTimesWithinATx() throws Exception {
    DocumentPdo existing = prepareEmptyDocument();
    existing.getAssociatedFacetData().put("foo", mapper.createObjectNode().put("bar", "baz"));
    
    mockExistingVersion(existing);

    Document updated = transactionTemplate.execute(status -> {
      Document update = prepareEmptyDocument();

      // update
      update.setVersionTimestamp(null);
      update.setDateModified(Instant.ofEpochMilli(123456L)); // some arbitrary marker date to check which
                                                 // version went through
      documentService.update(update);

      // update some previously set entity data
      entityStore.store(existing, "foo", mapper.createObjectNode().put("bar", "baz2"));

      // some more entity data
      entityStore.store(existing, "bar", mapper.createObjectNode().put("baz", "baz"));

      // start second update
      update = prepareEmptyDocument();
      update.setVersionTimestamp(null);
      update.setDateModified(Instant.ofEpochMilli(345678L)); // some arbitrary marker date to check which
                                                 // version went through
      return documentService.update(update).get();
    });

    // must be new version
    assertThat(updated.getVersionTimestamp(), greaterThan(existing.getVersionTimestamp()));

    // verify stored document
    DocumentPdo readBack = readBackFromStream(verifyStorePut());
    assertThat(readBack.getDocumentId(), equalTo(D));
    assertThat(readBack.getVersionTimestamp(), equalTo(updated.getVersionTimestamp()));
    assertThat(readBack.getDateModified(), equalTo(Instant.ofEpochMilli(345678L)));

    // must have both facet data sets
    assertThat(readBack.getSidecarElement("foo"), equalTo(mapper.createObjectNode().put("bar", "baz2")));
    assertThat(readBack.getSidecarElement("bar"), equalTo(mapper.createObjectNode().put("baz", "baz")));
  }
  
  @Test
  public void testThat_updatePreservesSidecar() throws Exception {
    // existing document with sidecar
    DocumentPdo existing = prepareEmptyDocument();
    existing.getAssociatedFacetData().put("foo", mapper.createObjectNode().put("bar", "baz"));
    
    mockExistingVersion(existing);

    // just some spurious update
    transactionTemplate.execute(status -> {
      return documentService.update(existing).get();
    });

    assertSchemaF(verifyStorePut());
  }

  private DocumentPdo anEmptyDocument() {
    DocumentPdo doc = new DocumentPdo();
    doc.setDocumentId(D);
    doc.setVersionTimestamp(Instant.now());
    return doc;
  }

  @Test(expected = DocumentAlreadyExistsException.class)
  public void testThat_documentCantBeCreatedTwice() throws Exception {
    Document doc = anEmptyDocument();

    mockExistingVersion(doc);

    documentService.createDocument(doc);
  }

  private ObjectName mockExistingVersion(final Document doc) {
    ObjectName metaPrefix = ObjectName.of(DOCUMENT_PREFIX, D, "meta");
    ObjectName name = metaPrefix.append(VERSION_FORMATTER.format(doc.getVersionTimestamp()));

    given(objectStoreService.list(eq(metaPrefix))).will(i -> Stream.of(new DocObject(mapper, doc, name)));
    given(objectStoreService.get(eq(name))).will(i -> new DocObject(mapper, doc, name));

    return name;
  }
  
  @Test(expected = VersionMismatchException.class)
  public void testThat_documentUpdateFailsOnVersionClashOnObjectStore() throws Exception {
    Document existing = prepareEmptyDocument();
    mockExistingVersion(existing);
    
    transactionTemplate.execute(status -> {
      // prime the tx cache with the current version
      documentService.getDocument(existing.getDocumentId());
      
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      
      // replace the mockery with a new version which appears as having changed outside the scope of the current transaction
      Document lostUpdateCandidate = prepareEmptyDocument();
      mockExistingVersion(lostUpdateCandidate);
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      
      // update has correct timestamp but we have a clash on the object store since it is no longer the current version
      Document update = prepareEmptyDocument();
      update.setVersionTimestamp(existing.getVersionTimestamp());
      return documentService.update(update);
    });
  }

  @Test
  public void testThat_documentExistsIsCorrect() throws IOException {
    Document doc = anEmptyDocument();

    mockExistingVersion(doc);

    assertThat(documentService.documentExists(D), equalTo(true));
    assertThat(documentService.documentExists("someNonexistentId"), equalTo(false));
  }
  
  @Test
  public void testThat_documentCanBeFound() throws IOException {
    Document doc = anEmptyDocument();

    ObjectName name = mockExistingVersion(doc);

    Document aDocument = documentService.getDocument(D).get();

    assertThat(aDocument.getDocumentId(), equalTo(D));

    verify(objectStoreService).get(name);
  }

  @Test
  public void testThat_documentDataCanBeDeleted() throws Exception {
    DocumentPdo existing = prepareEmptyDocument();
    existing.putSidecarElement("existing", mapper.createObjectNode().put("some", "thing"));

    mockExistingVersion(existing);

    transactionTemplate.execute(status -> {
      documentService.deleteDocument(D);
      return null;
    });

    // verify object store interaction
    DocumentPdo readBack = readBackFromStream(verifyStorePut());

    // verify stored document
    assertThat(readBack.getDocumentId(), equalTo(D));
    assertThat(readBack.isDeleted(), is(true));
  }
  
  @Test(expected = NotFoundException.class)
  public void testThat_deletedDocumentsCannotBeRetrievedAsCurrent() throws Exception {
    DocumentPdo deletionMarker = prepareEmptyDocument();
    deletionMarker.setDeleted(true);
    
    mockExistingVersion(deletionMarker);

    // should throw not found!
    documentService.getDocument(D).get();
  }

  
  @Test
  public void testThat_versionListCanBeRetrieved() throws IOException {
    mockThreeVersions();
    assertThat(documentService.getVersions(D), Matchers.contains(Instant.ofEpochMilli(1234567L),
        Instant.ofEpochMilli(2345678L), Instant.ofEpochMilli(3456789L)));
  }

  @Test
  public void testThat_cachedVersionListIsInvalidatedOnCreate() throws IOException {
    mockNonexistentDocument();
    
    // prime cache
    assertThat(documentService.getVersions(D), hasSize(0));
    
    mockExistingVersion(prepareEmptyDocument());

    // list supposed to be cached
    assertThat(documentService.getVersions(D), hasSize(0));
    
    // now simulate a create so that the list is invalidated
    mockNonexistentDocument();
    transactionTemplate.execute(c -> documentService.createDocument(prepareEmptyDocument()));
    mockExistingVersion(prepareEmptyDocument());
    
    // now the new size must be visible
    assertThat(documentService.getVersions(D), hasSize(1));
  }
  
  @Test
  public void testThat_cachedVersionListIsInvalidatedOnUpdate() throws IOException {
    Document docv1 = anEmptyDocument();
    docv1.setVersionTimestamp(Instant.ofEpochMilli(1234567L));
    Document docv2 = anEmptyDocument();
    docv2.setVersionTimestamp(Instant.ofEpochMilli(2345678L));
    
    ObjectName base = ObjectName.of(DOCUMENT_PREFIX, D);
    ObjectName meta = base.append("meta");
    ObjectName namev1 = meta.append(VERSION_FORMATTER.format(docv1.getVersionTimestamp()));
    ObjectName namev2 = meta.append(VERSION_FORMATTER.format(docv2.getVersionTimestamp()));
    
    given(objectStoreService.list(eq(meta))).will(i -> Stream.of( //
        new DocObject(mapper, docv1, namev1)
    ));
    given(objectStoreService.get(eq(namev1))).will(i -> new DocObject(mapper, docv1, namev1));
    
    // prime cache
    assertThat(documentService.getVersions(D), hasSize(1));
    
    // re-mock with two versions
    given(objectStoreService.list(eq(meta))).will(i -> Stream.of( //
        new DocObject(mapper, docv1, namev1),
        new DocObject(mapper, docv1, namev2)
    ));
    given(objectStoreService.get(eq(namev1))).will(i -> new DocObject(mapper, docv1, namev1));
    given(objectStoreService.get(eq(namev2))).will(i -> new DocObject(mapper, docv2, namev2));

    // list supposed to be cached
    assertThat(documentService.getVersions(D), hasSize(1));
    
    // now simulate an update so that the list is invalidated
    transactionTemplate.execute(c -> documentService.update(docv2));
    
    // now the new size must be visible
    assertThat(documentService.getVersions(D), hasSize(2));
  }
  
  @Test
  public void testThat_cachedVersionListIsInvalidatedOnDelete() throws IOException {
    mockExistingVersion(prepareEmptyDocument());
    
    // prime cache
    assertThat(documentService.getVersions(D), hasSize(1));
    
    mockNonexistentDocument();
    
    // list supposed to be cached
    assertThat(documentService.getVersions(D), hasSize(1));
    
    mockExistingVersion(prepareEmptyDocument());
    
    // now simulate a create so that the list is invalidated
    transactionTemplate.execute(c -> documentService.deleteDocument(D));
    
    mockNonexistentDocument();
    
    // now the new size must be visible
    assertThat(documentService.getVersions(D), hasSize(0));
  }

  @Test
  public void testThat_getReturnsTheCurrentVersion() throws IOException {
    mockThreeVersions();
    Document current = documentService.getDocument(D).get();
    assertThat(current.getVersionTimestamp(), is(Instant.ofEpochMilli(3456789L)));
    assertThat(entityStore.retrieve(current, "foo").get(), equalTo(mapper.createObjectNode().put("bar", "baz3")));
  }

  @Test
  public void testThat_oldVersionCanBeRetrieved() throws IOException {
    mockThreeVersions();
    Document old = documentService.getDocumentVersion(D, Instant.ofEpochMilli(2345678L)).get();
    assertThat(old.getVersionTimestamp(), equalTo(Instant.ofEpochMilli(2345678L)));
    assertThat(entityStore.retrieve(old, "foo").get(), equalTo(mapper.createObjectNode().put("bar", "baz2")));
  }

  @Test
  public void testThat_nonexistentVersionCannotBeRetrieved() throws IOException {
    mockThreeVersions();
    documentService.getDocumentVersion(D, Instant.ofEpochMilli(2345678L)).get();
  }

  private ObjectName mockThreeVersions() {
    DocumentPdo docv1 = anEmptyDocument();
    docv1.setVersionTimestamp(Instant.ofEpochMilli(1234567L));
    docv1.getAssociatedFacetData().put("foo", mapper.createObjectNode().put("bar", "baz1"));
    DocumentPdo docv2 = anEmptyDocument();
    docv2.setVersionTimestamp(Instant.ofEpochMilli(2345678L));
    docv2.getAssociatedFacetData().put("foo", mapper.createObjectNode().put("bar", "baz2"));
    DocumentPdo docv3 = anEmptyDocument();
    docv3.setVersionTimestamp(Instant.ofEpochMilli(3456789L));
    docv3.getAssociatedFacetData().put("foo", mapper.createObjectNode().put("bar", "baz3"));

    ObjectName base = ObjectName.of(DOCUMENT_PREFIX, D);
    ObjectName meta = base.append("meta");
    ObjectName namev1 = meta.append(VERSION_FORMATTER.format(docv1.getVersionTimestamp()));
    ObjectName namev2 = meta.append(VERSION_FORMATTER.format(docv2.getVersionTimestamp()));
    ObjectName namev3 = meta.append(VERSION_FORMATTER.format(docv3.getVersionTimestamp()));

    // the object store does not return the objects in order!
    given(objectStoreService.list(eq(meta))).will(i -> Stream.of( //
        new DocObject(mapper, docv3, namev3), //
        new DocObject(mapper, docv1, namev1), //
        new DocObject(mapper, docv2, namev2) //
    ));
    given(objectStoreService.get(eq(namev1))).will(i -> new DocObject(mapper, docv1, namev1));
    given(objectStoreService.get(eq(namev2))).will(i -> new DocObject(mapper, docv2, namev2));
    given(objectStoreService.get(eq(namev3))).will(i -> new DocObject(mapper, docv3, namev3));

    return base;
  }

  @Test(expected = IllegalStateException.class)
  public void testThat_mutationsRequireATransaction() throws Exception {
    Document doc = new Document();
    doc.setDocumentId("aDocument");
    documentService.createDocument(doc);
  }

  protected ArgumentCaptor<InputStream> verifyPersistOnce() {
    ArgumentCaptor<InputStream> isC = ArgumentCaptor.forClass(InputStream.class);
    verify(objectStoreService).put(any(), eq(ObjectStoreService.NEW_VERSION), isC.capture(), anyLong());
    verify(objectStoreService, atLeast(0)).get(any()); // don't care
    verify(objectStoreService, atLeastOnce()).list(any());
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
    mockNonexistentDocument();

    transactionTemplate.execute(status -> {
      Document doc = prepareEmptyDocument();
      entityStore.store(doc, "foo", mapper.createObjectNode().put("bar", "baz"));
      return documentService.createDocument(doc);
    });

    assertSchemaF(verifyPersistOnce());
  }

  private void mockNonexistentDocument() {
    given(objectStoreService.get(any())).willReturn(null);
    given(objectStoreService.list(any())).will(i -> Stream.of());
  }

  @Test
  public void testThat_documentDataCanBeAssociatedAfterCreate() throws Exception {
    mockNonexistentDocument();

    transactionTemplate.execute(status -> {
      Document doc = prepareEmptyDocument();
      Document created = documentService.createDocument(doc);
      entityStore.store(created, "foo", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });

    // In this particular case there won't be a get from the object store since the the service can tell
    // from the version list that it isn't there.
    assertSchemaF(verifyPersistOnce()); 
  }

  @Test
  public void testThat_documentDataCanBeAssociatedUponUpdate() throws Exception {
    mockExistingVersion(prepareEmptyDocument());

    Instant now = Instant.now();

    transactionTemplate.execute(status -> {
      Document toBeUpdated = documentService.getDocument(D).get();
      toBeUpdated.setDateModified(now);
      documentService.update(toBeUpdated);
      entityStore.store(toBeUpdated, "foo", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });

    Document doc = assertSchemaF(verifyPersistOnce());
    assertThat(doc.getDateModified(), equalTo(now));
  }

  @Test
  public void testThat_documentDataCanBeAssociatedWithExistingDocument() throws Exception {
    DocumentPdo existing = prepareEmptyDocument();
    existing.setAssociatedFacetData(new HashMap<>());
    existing.getAssociatedFacetData().put("preexisting", mapper.createObjectNode().put("foo", "bar"));
    
    mockExistingVersion(existing);

    transactionTemplate.execute(status -> {
      entityStore.store(existing, "foo", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });

    DocumentPdo persisted = assertSchemaF(verifyPersistOnce());
    assertThat(persisted.getSidecarElement("foo"), equalTo(mapper.createObjectNode().put("bar", "baz")));
  }

  @Test
  public void testThat_multipleAddsWork() throws Exception {
    Document existing = prepareEmptyDocument();

    mockExistingVersion(existing);

    transactionTemplate.execute(status -> {
      entityStore.store(existing, "foo", mapper.createObjectNode().put("bar", "baz"));
      entityStore.store(existing, "bar", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });

    DocumentPdo doc = assertSchemaF(verifyPersistOnce());
    assertThat(doc.getSidecarElement("bar"), equalTo(mapper.createObjectNode().put("bar", "baz")));
  }

  private DocumentPdo prepareEmptyDocument() {
    DocumentPdo doc = new DocumentPdo(D);
    doc.setVersionTimestamp(Instant.now());

    return doc;
  }

  @Test
  public void testThat_addAndRemoveCanBeCombined() throws Exception {
    DocumentPdo existing = prepareEmptyDocument();
    existing.putSidecarElement("existing", mapper.createObjectNode().put("some", "thing"));

    mockExistingVersion(existing);

    transactionTemplate.execute(status -> {
      entityStore.delete(existing, "existing");
      entityStore.store(existing, "foo", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });

    assertSchemaF(verifyPersistOnce());
  }

  @Test
  public void testThat_existingAssociatedDataIsPreserved() throws Exception {
    DocumentPdo existing = prepareEmptyDocument();
    existing.putSidecarElement("existing", mapper.createObjectNode().put("some", "thing"));

    mockExistingVersion(existing);

    transactionTemplate.execute(status -> {
      entityStore.store(existing, "foo", mapper.createObjectNode().put("bar", "baz"));
      return null;
    });

    DocumentPdo doc = assertSchemaF(verifyPersistOnce());
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
