package com.neverpile.eureka.rest.api.document;

import static com.neverpile.eureka.rest.api.document.MatchesPattern.matchesPattern;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.eureka.api.BaseTestConfiguration;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.impl.contentservice.SimpleContentElementService;
import com.neverpile.eureka.impl.documentservice.UuidDocumentIdGenerationStrategy;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.content.ContentElementFacet;
import com.neverpile.eureka.rest.api.document.content.ContentElementResource;
import com.neverpile.eureka.rest.api.document.core.IdFacet;
import com.neverpile.eureka.rest.configuration.ModelMapperConfiguration;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;

import io.restassured.http.ContentType;


/*
 * FIXME: there is some potential to factor this test class into a common abstract class with the
 * client tests. The latter run against a mock, static test Neverpile, though, which will support
 * far less realistic tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes=BaseTestConfiguration.class)
public class IdGenerationTest extends AbstractRestAssuredTest {
  private static final String UUID_PATTERN = "\\p{XDigit}{8}-(\\p{XDigit}{4}-){3}\\p{XDigit}{12}";

  @TestConfiguration
  @Import({
      ModelMapperConfiguration.class, SimpleContentElementService.class, ContentElementFacet.class,
      ContentElementResource.class, SimpleContentElementService.class, IdFacet.class, DocumentResource.class
  })
  public static class ServiceConfig {
    @Bean
    DocumentIdGenerationStrategy documentIdGenerationStrategy() {
      return new UuidDocumentIdGenerationStrategy();
    }
  }

  // Must mock the MultiVersioningDocumentService or we will break the app context initialization
  @MockBean
  MultiVersioningDocumentService mockDocumentService;

  @Autowired
  MockObjectStoreService mockObjectStoreService;

  @MockBean
  SimpleContentElementService mockContentElementService;

  /**
   * This method tests the successful creation of a unique id. It also tests if the id matches the
   * prescribed pattern (regex)
   */
  @Test
  public void testThat_documentCanBeCreatedWithGeneratedUUID() {
    DocumentDto dto = new DocumentDto();

    // @formatter:off
    ArgumentCaptor<Document> storedDocumentC = ArgumentCaptor.forClass(Document.class);
    
    BDDMockito
      .given(mockDocumentService.createDocument(storedDocumentC.capture()))
      .willAnswer(i -> i.getArgument(0));

    // store document
    DocumentDto resDoc = given()
        .accept(ContentType.JSON)
        .multiPart("doc", dto, ContentType.JSON.toString())
        .auth().preemptive().basic("user", "password")
      .when()
        .post("/api/v1/documents")
      .then()
        .log().all()
        .extract()
        .as(DocumentDto.class);

    
    assertThat(resDoc.getDocumentId(), matchesPattern(UUID_PATTERN));

    // store another one
    Document res2Doc = given()
        .accept(ContentType.JSON)
        .multiPart("doc", dto, ContentType.JSON.toString())
        .auth().preemptive().basic("user", "password")
      .when()
        .post("/api/v1/documents")
        .as(Document.class);

    assertThat(res2Doc.getDocumentId(), matchesPattern(UUID_PATTERN));
    // @formatter:on
    assertNotEquals(resDoc.getDocumentId(), res2Doc.getDocumentId());
  }
}
