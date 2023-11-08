package com.neverpile.eureka.search.elastic;

import static com.neverpile.common.condition.EqualsCondition.eq;
import static com.neverpile.eureka.api.index.DocumentQuery.query;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.core.MediaType;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.xcontent.XContentType;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mycila.xmltool.XMLDoc;
import com.neverpile.common.condition.AndCondition;
import com.neverpile.common.condition.Condition;
import com.neverpile.common.condition.EqualsCondition;
import com.neverpile.common.condition.RangeCondition;
import com.neverpile.eureka.api.index.DocumentQuery;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.plugin.metadata.rest.MetadataFacet;
import com.neverpile.eureka.plugin.metadata.service.Metadata;
import com.neverpile.eureka.plugin.metadata.service.MetadataElement;
import com.neverpile.eureka.rest.api.document.content.ContentElementFacet;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = ServiceConfig.class)
@Import({MetadataFacet.class, ContentElementFacet.class, ElasticsearchQueryService.class
})
@EnableAutoConfiguration
public class ElasticsearchQueryManualIT extends AbstractManualIT {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ElasticsearchQueryService elasticsearchQueryService;

  @Test
  public void testThat_objectsCanBeFoundUsingAQuery() throws IOException, InterruptedException {
    for (int i = 0; i < 20; i++) {
      client.index(new IndexRequest(testIndexName).id("id" + i).source(getTestDocMap(i), XContentType.JSON),
          RequestOptions.DEFAULT);
    }

    // wait for index to process
    Thread.sleep(1000);

    // single condition

    Condition cond0 = createEqualsCondition("documentId", "id1");

    DocumentQuery testQuery = createTestQuery(cond0);

    List<String> response = elasticsearchQueryService.searchDocument(testQuery, testIndexName);

    Assert.assertEquals(1, response.size());

    Assert.assertTrue(response.contains("id1"));

    // combined conditions

    Condition cond1 = createEqualsCondition("dateModified",
        Long.toString(Instant.ofEpochMilli(1500000000000L + (3 * 1000000000L)).toEpochMilli()));
    Condition cond2 = createRangeCondition("dateCreated",
        Long.toString(Instant.ofEpochMilli(1000000000000L + (2 * 1000000000L)).toEpochMilli()),
        Long.toString(Instant.ofEpochMilli(1000000000000L + (3 * 1000000000L)).toEpochMilli()));

    testQuery = createCombinedTestQuery(Arrays.asList(cond1, cond2));

    response = elasticsearchQueryService.searchDocument(testQuery, testIndexName);

    Assert.assertEquals(3, response.size());

    Assert.assertTrue(response.contains("id9"));
    Assert.assertTrue(response.contains("id10"));
    Assert.assertTrue(response.contains("id11"));
  }

  @Test
  public void testThat_metadataQueryingWorks() throws IOException, InterruptedException {
    // place one document with plenty of metadata into index
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
        .addText("'someTag' text value") //
        .gotoParent() //
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

    index.addDocument(doc, testIndexName);

    // wait for index to process
    Thread.sleep(1000);

    // query by string
    assertThat(
        elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someStringValue", "bar")).build(),
            testIndexName)).contains("id0");
    assertThat(
        elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someStringValue", "foo")).build(),
            testIndexName)).isEmpty();
    assertThat(elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someStringValue", 42)).build(),
        testIndexName)).isEmpty();

    // query by number
    assertThat(elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someIntegerValue", 42)).build(),
        testIndexName)).contains("id0");
    assertThat(
        elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someIntegerValue", 42.0)).build(),
            testIndexName)).isEmpty();
    assertThat(
        elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someIntegerValue", "foo")).build(),
            testIndexName)).isEmpty();

    // query by float
    assertThat(
        elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someFloatValue", 3.14159)).build(),
            testIndexName)).contains("id0");
    assertThat(elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someFloatValue", 3)).build(),
        testIndexName)).isEmpty();
    assertThat(
        elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someFloatValue", "3.14159")).build(),
            testIndexName)).isEmpty(); // !

    // query by boolean
    assertThat(
        elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someBooleanValue", true)).build(),
            testIndexName)).contains("id0");
    assertThat(
        elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someBooleanValue", false)).build(),
            testIndexName)).isEmpty();
    assertThat(
        elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someBooleanValue", "false")).build(),
            testIndexName)).isEmpty(); // !

    // query by child
    assertThat(elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someChild.foo", "bar")).build(),
        testIndexName)).contains("id0");
    assertThat(elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someChild.foo", "baz")).build(),
        testIndexName)).isEmpty();
    assertThat(
        elasticsearchQueryService.searchDocument(query(eq("metadata.jsonElement.someChild.notThere", "bar")).build(),
            testIndexName)).isEmpty();
  }

  private Condition createEqualsCondition(final String field, final String value) {
    EqualsCondition compCond = new EqualsCondition();
    compCond.withPredicate(field, value);
    return compCond;
  }

  private Condition createRangeCondition(final String field, final String begin, final String end) {
    RangeCondition rangeCond = new RangeCondition();

    List<Comparable<?>> list = new ArrayList<>();
    list.add(begin);
    list.add(end);
    rangeCond.withPredicate(field, list);
    return rangeCond;
  }

  private DocumentQuery createCombinedTestQuery(final List<Condition> condList) {
    AndCondition combinedCond = new AndCondition();
    for (Condition cond : condList) {
      combinedCond.withCondition(cond);
    }
    return new DocumentQuery(combinedCond, "documentId", DocumentQuery.SortOrder.ASC, 0, 50);
  }

  private DocumentQuery createTestQuery(final Condition cond) {
    return new DocumentQuery(new AndCondition().withCondition(cond), "documentId", DocumentQuery.SortOrder.ASC, 0, 50);
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
}