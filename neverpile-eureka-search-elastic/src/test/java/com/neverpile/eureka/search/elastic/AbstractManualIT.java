package com.neverpile.eureka.search.elastic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.neverpile.eureka.api.ContentElementService;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.model.HashAlgorithm;
import com.neverpile.eureka.plugin.metadata.service.MetadataService;


public abstract class AbstractManualIT {

  protected static final String MAPPING_NAME = "_doc";

  @MockBean
  protected DocumentService mockDocumentService;

  @MockBean
  protected DocumentIdGenerationStrategy mockDocumentIdGenerationStrategy;

  @Autowired
  protected RestHighLevelClient client;

  @Autowired
  protected ElasticsearchDocumentIndex index;

  @MockBean
  protected MetadataService metadataService;

  @MockBean
  protected ContentElementService contentElementService;

  protected static String testIndexName;

  protected static List<Document> listOfDocuments = new ArrayList<>();

  @BeforeClass
  public static void setUp() {
    for (int i = 0; i < 60; i++) {
      listOfDocuments.add(createTestDocumentWithContent(i));
    }
  }

  private static Document createTestDocumentWithContent(final int id) {
    Document document = new Document("id" + id);
    document.setDateCreated(new Date(1000000000000L + ((id / 3) * 1000000000L)));
    document.setDateModified(new Date(1500000000000L + ((id / 3) * 1000000000L)));

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

    ArrayList<ContentElement> contentElements = new ArrayList<>();
    contentElements.add(ce0);
    contentElements.add(ce1);
    document.setContentElements(contentElements);
    return document;
  }

  @Before
  public void prepare() throws IOException {
    index.ensureIndexUpToDateOrRebuildInProgress();
    testIndexName = index.getIndexNameFromAlias(ElasticsearchDocumentIndex.INDEX_ALIAS_READ);
  }
}