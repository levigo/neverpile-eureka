package com.neverpile.eureka.search.elastic;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycila.xmltool.XMLDoc;
import com.neverpile.eureka.api.index.IndexMaintenanceService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.plugin.metadata.service.Metadata;
import com.neverpile.eureka.plugin.metadata.service.MetadataElement;

public abstract class AbstractIndexMaintenanceIT extends AbstractManualIT {

  @Autowired
  IndexMaintenanceService indexMaintenanceService;

  @Autowired
  private ObjectMapper objectMapper;

  public abstract void waitOrNot();

  @Before
  @Override
  public void prepare() throws IOException {
    index.ensureIndexUpToDateOrRebuildInProgress();
    waitOrNot();
    testIndexName = index.getIndexNameFromAlias(ElasticsearchDocumentIndex.INDEX_ALIAS_READ);
    waitOrNot();
  }


  @Test
  public void testThat_newObjectsCanBeInsertedIntoTheIndex() throws IOException {
    BDDMockito.given(mockDocumentService.getDocument(any())).willReturn(Optional.of(listOfDocuments.get(0)));

    indexMaintenanceService.indexDocument(listOfDocuments.get(0));
    waitOrNot();

    assertTrue(client.get(new GetRequest(testIndexName, listOfDocuments.get(0).getDocumentId()),
        RequestOptions.DEFAULT).isExists());
  }

  @Test
  public void testThat_objectsCanBeDeletedFromTheIndex() throws IOException {
    BDDMockito.given(mockDocumentService.getDocument(any())).willReturn(Optional.of(listOfDocuments.get(0)));

    indexMaintenanceService.indexDocument(listOfDocuments.get(0));
    waitOrNot();

    assertTrue(client.get(new GetRequest(testIndexName, listOfDocuments.get(0).getDocumentId()),
        RequestOptions.DEFAULT).isExists());

    indexMaintenanceService.deleteDocument(listOfDocuments.get(0).getDocumentId());
    waitOrNot();

    assertFalse(client.get(new GetRequest(testIndexName, listOfDocuments.get(0).getDocumentId()),
        RequestOptions.DEFAULT).isExists());
  }

  @Test
  public void testThat_IndexEntryCanBeUpdated() throws IOException, InterruptedException {
    client.index(new IndexRequest(testIndexName).id("id" + 0).source(getTestDocMap(0), XContentType.JSON),
        RequestOptions.DEFAULT);

    GetResponse response = client.get(new GetRequest(testIndexName, listOfDocuments.get(0).getDocumentId()),
        RequestOptions.DEFAULT);
    Assert.assertEquals(1000000000000L, response.getSource().get("dateCreated"));

    Document doc = new Document(listOfDocuments.get(0).getDocumentId());
    doc.setDateCreated(Instant.ofEpochMilli(1100000000000L));

    BDDMockito.given(mockDocumentService.getDocument(any())).willReturn(Optional.of(doc));

    indexMaintenanceService.updateDocument(doc);
    waitOrNot();

    response = client.get(new GetRequest(testIndexName, listOfDocuments.get(0).getDocumentId()),
        RequestOptions.DEFAULT);
    Assert.assertEquals(1100000000000L, response.getSource().get("dateCreated"));
  }

  @Test
  public void testThat_IndexCanBeHardResetted() throws IOException {
    client.index(new IndexRequest(testIndexName).id("id" + 0).source(getTestDocMap(0), XContentType.JSON),
        RequestOptions.DEFAULT);
    GetRequest getRequest = new GetRequest(testIndexName, listOfDocuments.get(0).getDocumentId());
    assertTrue(client.get(getRequest, RequestOptions.DEFAULT).isExists());
    indexMaintenanceService.hardResetIndex();
    waitOrNot();
    // new index name after reset
    testIndexName = index.getIndexNameFromAlias(ElasticsearchDocumentIndex.INDEX_ALIAS_READ);
    getRequest = new GetRequest(testIndexName, listOfDocuments.get(0).getDocumentId());
    assertFalse(client.get(getRequest, RequestOptions.DEFAULT).isExists());
  }


  @Test
  public void testThat_IndexCanBeRebuilt() throws IOException, InterruptedException {
    ArrayList<String> ids = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      client.index(new IndexRequest(testIndexName).id("id" + i).source(getTestDocMap(i), XContentType.JSON),
          RequestOptions.DEFAULT);
      ids.add("id" + i);
    }

    BDDMockito.given(mockDocumentService.getAllDocumentIds()).willReturn(ids.stream());
    BDDMockito.given(mockDocumentService.getDocument(any())).willAnswer(i -> {
      String docId = i.getArgument(0);
      Document d = new Document();
      d.setDocumentId(docId);
      return Optional.of(d);
    });
    BDDMockito.given(mockDocumentService.getDocuments(any())).willAnswer(i -> {
      List<String> docIds = i.getArgument(0);
      return docIds.stream().map(id -> {
        Document d = new Document();
        d.setDocumentId(id);
        return d;
      }).collect(Collectors.toList());
    });

    // wait for index to process
    Thread.sleep(2000);

    indexMaintenanceService.rebuildIndex();
    waitOrNot();
    // wait for index to process
    Thread.sleep(2000);

    // new index name after rebuild
    testIndexName = index.getIndexNameFromAlias(ElasticsearchDocumentIndex.INDEX_ALIAS_READ);

    CountRequest countRequest = new CountRequest(testIndexName);
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.query(QueryBuilders.matchAllQuery());
    countRequest.source(searchSourceBuilder);
    CountResponse countResponse = client.count(countRequest, RequestOptions.DEFAULT);
    ;

    Assert.assertEquals(20, countResponse.getCount());
  }

  private Map<String, Object> getTestDocMap(final int index) {
    if (index >= listOfDocuments.size()) {
      System.err.println("call to \"getTestDocMap()\" with invalid Index");
      return new HashMap<>();
    }
    Map<String, Object> json = new HashMap<>();
    json.put("documentId", listOfDocuments.get(index).getDocumentId());
    json.put("dateCreated", listOfDocuments.get(index).getDateCreated().toEpochMilli());
    json.put("dateModified", listOfDocuments.get(index).getDateModified().toEpochMilli());
    return json;
  }

  @Test
  @Ignore
  public void testThat_metadataIndexingIsCorrect() throws Exception {
    Document doc = listOfDocuments.get(0);

    ObjectNode metadataJson = JsonNodeFactory.instance.objectNode() //
        .put("someStringValue", "bar") //
        .put("someIntegerValue", 42) //
        .put("someFloatValue", 3.14159) //
        .put("someLongValue", Long.MAX_VALUE) //
        .put("someBooleanValue", true) //
        .putNull("someNullValue") //
        .put("someDateValue", Instant.ofEpochMilli(12345678).toEpochMilli()); // FIXME
    metadataJson.putObject("someChild").put("foo", "bar");
    metadataJson.putArray("someArray").add("foo").add("bar").add("baz");

    MetadataElement jsonElement = new MetadataElement();
    jsonElement.setContent(objectMapper.writeValueAsBytes(metadataJson));
    jsonElement.setContentType(MediaType.APPLICATION_JSON_TYPE);
    jsonElement.setSchema("mySchema");
    jsonElement.setEncryption(EncryptionType.SHARED);

    MetadataElement xmlElement = new MetadataElement();
    xmlElement.setContent(XMLDoc.newDocument().addRoot("root") //
        .addTag("someTag") //
        .addAttribute("anAttribute", "anAttributeValue") //
        .addAttribute("anotherAttribute", "anotherAttributeValue") //
        .addText("first 'someTag' text value") //
        .gotoParent() //
        .addTag("someTag") //
        .addText("second 'someTag' text value") //
        .addTag("someNestedTag") //
        .addAttribute("yetAnotherAttribute", "yetAnotherAttributeValue") //
        .toBytes());
    xmlElement.setContentType(MediaType.APPLICATION_XML_TYPE);
    xmlElement.setSchema("mySchema");
    xmlElement.setEncryption(EncryptionType.SHARED);

    Metadata metadata = new Metadata();
    metadata.put("jsonElement", jsonElement);
    metadata.put("xmlElement", xmlElement);

    BDDMockito.given(metadataService.get(doc)).willReturn(Optional.of(metadata));
    BDDMockito.given(mockDocumentService.getDocument(any())).willReturn(Optional.of(doc));

    indexMaintenanceService.indexDocument(doc);
    waitOrNot();

    // wait for index to process
    waitOrNot();

    GetResponse value = client.get(new GetRequest(testIndexName, doc.getDocumentId()), RequestOptions.DEFAULT);
    assertNotNull(value);

    assertThat(objectMapper.readTree(value.getSourceAsString()).get("metadata"), equalTo(objectMapper.readTree(
        // @formatter:off
        "{" +
        "  \"jsonElement\": {" + 
        "   \"someStringValue_text\": \"bar\"," + 
        "   \"someIntegerValue_int\": 42," + 
        "   \"someFloatValue_float\": 3.14159," + 
        "   \"someLongValue_int\": 9223372036854775807," + 
        "   \"someBooleanValue_bool\": true," + 
        "   \"someNullValue_null\": null," + 
        "   \"someDateValue_int\": 12345678," + 
        "   \"someChild\": {" + 
        "     \"foo_text\": \"bar\"" + 
        "   }," + 
        "   \"someArray\": [" + 
        "     \"foo\"," + 
        "     \"bar\"," + 
        "     \"baz\"" + 
        "   ]" + 
        " }," + 
        " \"xmlElement\": {" + 
        "   \"someTag_text\": \"second 'someTag' text value\"," + 
        "   \"someNestedTag\": {" + 
        "     \"yetAnotherAttribute_text\": \"yetAnotherAttributeValue\"" + 
        "   }" + 
        " }" + 
        "}")));
    // @formatter:on
  }

  /**
   * Indexes the same metadata element with two conflicting mappings. Typeification during indexing
   * must prevent the conflict.
   *
   * @throws Exception
   */
  @Test
  public void testThat_metadataIndexingResolvesTypeConflicts() throws Exception {
    Document doc = listOfDocuments.get(0);

    MetadataElement jsonElement = new MetadataElement();
    // someValue is a number
    jsonElement.setContent(
        objectMapper.writeValueAsBytes(JsonNodeFactory.instance.objectNode().put("someValue", 12345)));
    jsonElement.setContentType(MediaType.APPLICATION_JSON_TYPE);
    jsonElement.setSchema("mySchema");
    jsonElement.setEncryption(EncryptionType.SHARED);

    Metadata metadata = new Metadata();
    metadata.put("jsonElement", jsonElement);

    BDDMockito.given(mockDocumentService.getDocument(any())).willReturn(Optional.of(doc));
    BDDMockito.given(metadataService.get(doc)).willReturn(Optional.of(metadata));

    indexMaintenanceService.indexDocument(doc);
    waitOrNot();

    Document doc1 = listOfDocuments.get(1);
    // someValue is now a string
    jsonElement.setContent(
        objectMapper.writeValueAsBytes(JsonNodeFactory.instance.objectNode().put("someValue", "i am now a string")));

    BDDMockito.given(mockDocumentService.getDocument(any())).willReturn(Optional.of(doc1));
    BDDMockito.given(metadataService.get(doc1)).willReturn(Optional.of(metadata));

    indexMaintenanceService.indexDocument(doc1);
    waitOrNot();

    GetResponse value = client.get(new GetRequest(testIndexName, doc.getDocumentId()), RequestOptions.DEFAULT);
    assertNotNull(value);

    assertThat(objectMapper.readTree(value.getSourceAsString()).get("metadata"), equalTo(objectMapper.readTree(
        // @formatter:off
        "{" +
        "  \"jsonElement\": {" + 
        "   \"someValue_int\": 12345" +
        " }" +
        "}")));
    // @formatter:on

    value = client.get(new GetRequest(testIndexName, doc1.getDocumentId()), RequestOptions.DEFAULT);
    assertNotNull(value);

    assertThat(objectMapper.readTree(value.getSourceAsString()).get("metadata"), equalTo(objectMapper.readTree(
        // @formatter:off
        "{" +
        "  \"jsonElement\": {" + 
        "   \"someValue_text\": \"i am now a string\"" +
        " }" +
        "}")));
    // @formatter:on
  }


  @Test
  public void testThat_contentElementIndexingIsCorrect() throws Exception {
    Document doc = listOfDocuments.get(0);

    BDDMockito.given(mockDocumentService.getDocument(any())).willReturn(Optional.of(doc));

    indexMaintenanceService.indexDocument(doc);
    waitOrNot();

    GetResponse value = client.get(new GetRequest(testIndexName, doc.getDocumentId()), RequestOptions.DEFAULT);
    assertNotNull(value);

    assertThat(objectMapper.readTree(value.getSourceAsString()).get("contentElements"), equalTo(objectMapper.readTree(
        // @formatter:off
        "[" + 
        " {\"id\":\"2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae\",\"role\":\"part\",\"fileName\":\"foo.txt\",\"mediaType\":\"text/plain\",\"length\":3}," + 
        " {\"id\":\"4938d873b6755092912b54f97033052206192a4eaae5ce9a4f235a1067d04b0d\",\"role\":\"annotations\",\"fileName\":\"foo.xml\",\"mediaType\":\"application/xml\",\"length\":17}" + 
        "]")));
    // @formatter:on
  }

  @Test
  public void testThat_IndexAliasCanBeSet() throws IOException {
    index.setAliasForIndex(testIndexName, "test_alias");
    client.index(new IndexRequest("test_alias").id("id" + 0).source(getTestDocMap(0), XContentType.JSON),
        RequestOptions.DEFAULT);
    GetResponse response = client.get(new GetRequest(testIndexName, listOfDocuments.get(0).getDocumentId()),
        RequestOptions.DEFAULT);
    Assert.assertNotNull(response);
  }

  @Test(expected = ElasticsearchStatusException.class)
  public void testThat_IndexAliasCanBeRemoved() throws IOException {
    index.setAliasForIndex(testIndexName, "test_alias");
    client.index(new IndexRequest("test_alias").id("id" + 0).source(getTestDocMap(0), XContentType.JSON),
        RequestOptions.DEFAULT);
    GetResponse response = client.get(new GetRequest("test_alias", listOfDocuments.get(0).getDocumentId()),
        RequestOptions.DEFAULT);
    Assert.assertNotNull(response);
    index.removeAliasForIndex(testIndexName, "test_alias");
    // should throw exception
    client.get(new GetRequest("test_alias", listOfDocuments.get(0).getDocumentId()), RequestOptions.DEFAULT);
  }
}