package com.neverpile.eureka.search.elastic;

import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neverpile.common.specifier.Specifier;
import com.neverpile.common.util.DevNullOutputStream;
import com.neverpile.common.util.VisibleForTesting;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.api.index.Array;
import com.neverpile.eureka.api.index.Field;
import com.neverpile.eureka.api.index.Field.Type;
import com.neverpile.eureka.api.index.IndexMaintenanceException;
import com.neverpile.eureka.api.index.Schema;
import com.neverpile.eureka.api.index.Structure;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentFacet;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class ElasticsearchDocumentIndex {
  private static final Pattern INDEX_NAME_PATTERN = Pattern.compile("document-(\\d+)-(\\p{XDigit}+)-(\\p{XDigit}+)");
  static String INDEX_ALIAS_READ = "document-read";
  static String INDEX_ALIAS_WRITE = "document-write";

  private static final String ROOT_NAME = "$$ROOT";

  private final static Logger LOGGER = LoggerFactory.getLogger(ElasticsearchDocumentIndex.class);

  private static final String SCHEMA_MAPPING_VERSION = "1";

  @Autowired
  private final List<DocumentFacet<?>> facets = new ArrayList<>();

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private DocumentService documentService;

  @Autowired
  private RestHighLevelClient client;

  @Autowired
  private MeterRegistry meterRegistry;

  private final AtomicBoolean rebuildActive = new AtomicBoolean(false);

  private Counter indexSuccessCounter;

  private Counter indexFailureCounter;

  private Structure indexSchema;

  @PostConstruct
  public void init() {
    indexSuccessCounter = meterRegistry.counter("elastic.index.success");
    indexFailureCounter = meterRegistry.counter("elastic.index.failure");

    indexSchema = createIndexSchema();
  }

  public void addDocument(final Document doc, final String indexName) {
    IndexRequest indexRequest = new IndexRequest(indexName);
    indexRequest.id(doc.getDocumentId());
    indexRequest.source(getFacetIndexData(doc), XContentType.JSON);
    try {
      client.index(indexRequest, RequestOptions.DEFAULT);
      indexSuccessCounter.increment();
    } catch (Exception e) {
      indexFailureCounter.increment();
      throw new IndexMaintenanceException("Indexing failed", e);
    }
  }

  public void updateDocument(final Document doc, final String indexName) {
    UpdateRequest updateRequest = new UpdateRequest(indexName, doc.getDocumentId());
    updateRequest.doc(getFacetIndexData(doc));
    try {
      client.update(updateRequest, RequestOptions.DEFAULT);
      indexSuccessCounter.increment();
    } catch (Exception e) {
      indexFailureCounter.increment();
      throw new IndexMaintenanceException("Index update failed", e);
    }
  }

  public void deleteDocument(final String documentId, final String indexName) {
    DeleteRequest deleteRequest = new DeleteRequest(indexName, documentId);
    try {
      client.delete(deleteRequest, RequestOptions.DEFAULT);
      indexSuccessCounter.increment();
    } catch (Exception e) {
      indexFailureCounter.increment();
    }
  }

  @Async
  public void hardResetIndex() {
    DeleteIndexRequest request = null;
    try {
      request = new DeleteIndexRequest(getIndexNameFromAlias(INDEX_ALIAS_WRITE),
          getIndexNameFromAlias(INDEX_ALIAS_READ));
      client.indices().delete(request, RequestOptions.DEFAULT);

      ensureIndexUpToDateOrRebuildInProgress();
    } catch (IOException e) {
      throw new IndexMaintenanceException("Index reset failed", e);
    }
  }

  @Async
  public void rebuildIndex() {
    LOGGER.info("Index rebuild started.");
    rebuildActive.set(true);

    try {
      String obsoleteIndexName = getIndexNameFromAlias(INDEX_ALIAS_READ);

      String inProgressIndexName = createIndex();
      setAliasForIndex(inProgressIndexName, INDEX_ALIAS_WRITE);

      try {
        LOGGER.info("Starting index rebuild");

        AtomicInteger indexedDocuments = new AtomicInteger();
        AtomicLong lastLogMessage = new AtomicLong(System.currentTimeMillis());
        Stream<String> stream = documentService.getAllDocumentIds();
        long indexed = stream //
            .map(d -> documentService.getDocument(d).orElse(null)) //
            .filter(Objects::nonNull) //
            .peek(d -> {
              addDocument(d, inProgressIndexName);
              indexedDocuments.incrementAndGet();

              long last = lastLogMessage.get();
              if (last < System.currentTimeMillis() - 10000
                  && lastLogMessage.compareAndSet(last, System.currentTimeMillis())) {
                LOGGER.info("Indexed {} documents", indexedDocuments);
              }
            }).count();
        LOGGER.info("Index rebuild completed with {} elements.", indexed);

        // switch active index
        setAliasForIndex(inProgressIndexName, INDEX_ALIAS_READ);

        // delete old one
        if (null != obsoleteIndexName) {
          deleteIndex(obsoleteIndexName);
        }
        LOGGER.info("Index rebuild finished");
      } catch (Exception e) {
        LOGGER.error("Index rebuild failed", e);

        try {
          // switch active index
          if (null != obsoleteIndexName) {
            setAliasForIndex(obsoleteIndexName, INDEX_ALIAS_WRITE);
          }

          // delete the one we tried to create
          deleteIndex(inProgressIndexName);
        } catch (IOException f) {
          LOGGER.error("Also, failed to cleanup after rebuild failure", e);
        }

        throw new IndexMaintenanceException("Index rebuild failed", e);
      }
    } catch (Exception e) {
      LOGGER.error("Failed to (re)bulild index", e);
    }
  }

  String createIndex() throws IOException {
    Structure schema = createIndexSchema();
    schema.setName(ROOT_NAME);

    ObjectNode mapping = schemaToMapping(schema);

    String name = generateNewIndexName(mapping);

    LOGGER.info("Creating index {}", name);

    return createIndex(mapping, name);
  }

  String generateNewIndexName(final ObjectNode mapping) throws IOException {
    return "document-" + SCHEMA_MAPPING_VERSION + "-" + schemaHash(mapping) + "-"
        + Long.toHexString(System.currentTimeMillis());
  }

  public String createIndex(final ObjectNode mapping, final String indexName) throws IOException {
    CreateIndexRequest request = new CreateIndexRequest(indexName);

    request.mapping(objectMapper.writeValueAsString(mapping), XContentType.JSON);

    client.indices().create(request, RequestOptions.DEFAULT);

    return indexName;
  }

  void deleteIndex(final String indexName) throws IOException {
    DeleteIndexRequest request = new DeleteIndexRequest(indexName);
    client.indices().delete(request, RequestOptions.DEFAULT);
  }

  @VisibleForTesting
  void setAliasForIndex(final String indexName, final String alias) throws IOException {
    IndicesAliasesRequest request = new IndicesAliasesRequest();
    String currentIndex = getIndexNameFromAlias(alias);
    if (null != currentIndex) {
      request.addAliasAction(IndicesAliasesRequest.AliasActions.remove().index(currentIndex).alias(alias));
    }
    request.addAliasAction(IndicesAliasesRequest.AliasActions.add().index(indexName).alias(alias));
    try {
      client.indices().updateAliases(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @VisibleForTesting
  void removeAliasForIndex(final String indexName, final String alias) {
    IndicesAliasesRequest request = new IndicesAliasesRequest();
    request.addAliasAction(IndicesAliasesRequest.AliasActions.remove().index(indexName).alias(alias));
    try {
      client.indices().updateAliases(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  ObjectNode schemaToMapping(final Schema rootSchema) throws IOException {
    ObjectNode m = objectMapper.createObjectNode();

    schemaToMapping(rootSchema, true, m);

    return m;
  }

  private void schemaToMapping(final Schema s, final boolean isRoot, final ObjectNode elementNode) {
    if (s instanceof Structure) {
      Structure t = (Structure) s;

      if (!isRoot)
        elementNode.put("type", "object");
      elementNode.put("dynamic", t.isDynamic() ? "true" : "false");

      t.getElements().stream().sorted().forEach(
          e -> schemaToMapping(e, false, elementNode.with("properties").putObject(e.getName())));
    } else if (s instanceof Array) {
      Array a = (Array) s;
      schemaToMapping(a.getElementSchema(), false, elementNode);
    } else if (s instanceof Field) {
      Field f = (Field) s;

      // primary type
      elementNode.put("type", toElasticType(f.getType()));

      // alternative types
      if (f.getAlternativeTypes() != null && f.getAlternativeTypes().length > 0) {
        ObjectNode multiFieldNode = elementNode.putObject("fields");
        for (Type a : f.getAlternativeTypes()) {
          multiFieldNode.put(a.name().toLowerCase(), toElasticType(a));
        }
      }
    }
  }

  /**
   * Map a schema {@link Type} to its equivalent type in elasticsearch.
   *
   * @param t the schema type
   * @return the elastic type
   */
  private String toElasticType(final Type t) {
    switch (t){
      case Text :
      case Boolean :
      case Binary :
      case Keyword :
        return t.name().toLowerCase();

      case Date :
      case DateTime :
      case Time :
        return "date";

      case Integer :
        return "long";

      case Number :
        return "double";

      default :
        // FIXME: probably wrong
        return "object";
    }
  }

  String getIndexNameFromAlias(final String aliasName) throws IOException {
    return client.indices() //
        .getAlias(new GetAliasesRequest(aliasName), RequestOptions.DEFAULT) //
        .getAliases() // Map<String, Set<AliasMetaData>> with single entry pointing to index
        .keySet().stream().findAny().orElse(null);
  }

  @Async
  public void ensureIndexUpToDateOrRebuildInProgress() {
    try {
      Schema schema = createIndexSchema();

      String expectedHash = schemaHash(schemaToMapping(schema));

      if (verifyMapping(expectedHash, INDEX_ALIAS_READ, true)) {
        LOGGER.info("Current schema mapping is up to date");
        return;
      }

      if (verifyMapping(expectedHash, INDEX_ALIAS_WRITE, true)) {
        LOGGER.info("Current schema mapping is outdated, but rebuild seems to be in progress");
        return;
      }
    } catch (IOException e) {
      LOGGER.info("Index not found, creating new index...");
    }

    // needs rebuild
    rebuildIndex();
  }

  private boolean verifyMapping(final String expectedHash, final String aliasName, final boolean logInfo)
      throws IOException {
    // retrieve index alias by name
    String indexName = getIndexNameFromAlias(aliasName);
    if (null == indexName) {
      if (logInfo)
        LOGGER.info("Index alias not found", indexName);
      return false;
    }

    // match index name against pattern
    Matcher m = INDEX_NAME_PATTERN.matcher(indexName);
    if (!m.matches()) {
      LOGGER.warn("Index name format not of the expected form document-<VERSION>-<HASH>-<TIMESTAMP>: {}", indexName);
      return false;
    }

    String schemaVersion = m.group(1);
    String schemaHash = m.group(2);

    // verify has and version
    boolean match = expectedHash.equals(schemaHash) && SCHEMA_MAPPING_VERSION.equals(schemaVersion);
    if (logInfo && !match)
      LOGGER.info("Schema mismatch - expected schema version {} with hash {}, but got {} / {}", SCHEMA_MAPPING_VERSION,
          expectedHash, schemaVersion, schemaHash);

    return match;
  }

  private String schemaHash(final JsonNode json) throws IOException {
    try {
      MessageDigest md5 = MessageDigest.getInstance("md5");
      try (DigestOutputStream dos = new DigestOutputStream(new DevNullOutputStream(), md5)) {
        objectMapper.writeValue(dos, json);
        return new String(Hex.encode(md5.digest()));
      }
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Should not happen: MD5 not available");
    }
  }

  Structure createIndexSchema() {
    return new Structure(ROOT_NAME, //
        facets.stream().map(f -> {
          Schema schema = f.getIndexSchema();
          if (null != schema)
            schema.setName(f.getName());
          return schema;
        }).filter(Objects::nonNull).collect(Collectors.toSet()) //
    );
  }

  JsonNode getCurrentMapping(final String aliasName) throws IOException {
    String indexName = getIndexNameFromAlias(aliasName);
    if (null == indexName)
      return null;

    GetMappingsResponse mapping = client.indices().getMapping((new GetMappingsRequest()).indices(indexName),
        RequestOptions.DEFAULT);
    CompressedXContent xContent = mapping.mappings().get(indexName).source();
    return objectMapper.readTree(xContent.uncompressed().streamInput());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getFacetIndexData(final Document document) {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode dataNode = objectMapper.createObjectNode();
    for (DocumentFacet<?> facet : facets) {
      JsonNode facetIndexData = facet.getIndexData(document);
      if (facetIndexData != null) {
        dataNode.set(facet.getName(), typeify(facetIndexData, Specifier.from(facet.getName())));
      }
    }
    return objectMapper.convertValue(dataNode, Map.class);
  }

  /**
   * Suffix indexed field names with an indicator for the value type. This is needed for dynamic
   * index elements in order to prevent the automatic type inferral to cause incompatibility errors
   * when some element is indexed with varying value types.
   *
   * @param node the index structure node to "typeify"
   * @param path the JSON path we're at
   * @return the "typeified" node
   */
  private JsonNode typeify(JsonNode node, final Specifier path) {
    if (null == node)
      return node;

    if (node.isArray()) {
      ArrayNode typeifiedArray = objectMapper.createArrayNode();
      ((ArrayNode) node).elements().forEachRemaining(n -> typeifiedArray.add(typeify(n, path)));
      node = typeifiedArray;
    } else if (node.isObject()) {
      node = typeify((ObjectNode) node, path);
    }

    return node;
  }

  /**
   * Suffix indexed field names with an indicator for the value type. This is needed for dynamic
   * index elements in order to prevent the automatic type inferral to cause incompatibility errors
   * when some element is indexed with varying value types.
   *
   * @param node the index structure node to "typeify"
   * @param path the JSON path we're at
   * @return the "typeified" node
   */
  private ObjectNode typeify(final ObjectNode node, final Specifier path) {
    if (null == node)
      return node;

    // don't typeify unless the schema path is dynamic
    if (!indexSchema.isDynamicBranch(path))
      return node;

    ObjectNode typeified = objectMapper.createObjectNode();
    for (Iterator<Entry<String, JsonNode>> i = node.fields(); i.hasNext();) {
      Entry<String, JsonNode> e = i.next();
      JsonNode v = e.getValue();
      if (v.isTextual())
        typeified.set(e.getKey() + "_text", v);
      else if (v.isFloatingPointNumber())
        typeified.set(e.getKey() + "_float", v);
      else if (v.isIntegralNumber())
        typeified.set(e.getKey() + "_int", v);
      else if (v.isBoolean())
        typeified.set(e.getKey() + "_bool", v);
      else if (v.isNull())
        typeified.set(e.getKey() + "_null", v);
      else if (v.isArray()) {
        ArrayNode typeifiedArray = objectMapper.createArrayNode();
        ((ArrayNode) v).elements().forEachRemaining(n -> typeifiedArray.add(typeify(n, path.append(e.getKey()))));
        typeified.set(e.getKey(), typeifiedArray);
      } else if (v.isObject()) {
        typeified.set(e.getKey(), typeify((ObjectNode) v, path.append(e.getKey())));
      }
    }

    return typeified;
  }

  public Structure getIndexSchema() {
    return indexSchema;
  }
}