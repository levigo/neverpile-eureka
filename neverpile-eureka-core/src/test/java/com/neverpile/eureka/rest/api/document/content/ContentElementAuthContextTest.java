package com.neverpile.eureka.rest.api.document.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.Optional;

import jakarta.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.common.authorization.api.AuthorizationContext;
import com.neverpile.common.authorization.api.AuthorizationService;
import com.neverpile.eureka.api.BaseTestConfiguration;
import com.neverpile.eureka.api.ContentElementService;
import com.neverpile.eureka.api.DocumentAuthorizationService;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.impl.authorization.DefaultDocumentAuthorizationService;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.configuration.DocumentFacetAuthorizationContextContributor;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.test.AbstractRestAssuredTest;


/*
 * FIXME: there is some potential to factor this test class into a common abstract class with the
 * client tests. The latter run against a mock, static test Neverpile, though, which will support
 * far less realistic tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes=BaseTestConfiguration.class)
public class ContentElementAuthContextTest extends AbstractRestAssuredTest {
  @TestConfiguration
  @Import({
      DocumentFacetAuthorizationContextContributor.class, ContentElementFacet.class
  })
  public static class ServiceConfig {
    @Bean
    public DocumentAuthorizationService documentAuthorizationService() {
      return new DefaultDocumentAuthorizationService();
    }
  }

  // Must mock the MultiVersioningDocumentService or we will break the app context initialization
  @MockBean
  MultiVersioningDocumentService mockDocumentService;

  @MockBean
  EventPublisher eventPublisher;

  @Autowired
  MockObjectStoreService mockObjectStoreService;

  @MockBean
  AuthorizationService authService;

  @MockBean
  ContentElementService contentElementService;

  @Autowired
  DocumentAuthorizationService documentAuthorizationService;

  @Before
  public void initMock() {
    // provide dummy document
    given(mockDocumentService.getDocument(any())).willAnswer(i -> {
      return Optional.of(createTestDocument(i.getArgument(0)));
    });
    given(mockDocumentService.documentExists(any())).willReturn(true);
  }

  private Document createTestDocument(final String id) {
    Document doc = new Document(id);


    ContentElement ce1 = new ContentElement();
    ce1.setContentElementId("foo1");
    ce1.setRole("foo");
    ce1.setType(MediaType.TEXT_PLAIN_TYPE);
    ce1.setLength(12345);
    ce1.setFileName("foo.txt");

    ContentElement ce2 = new ContentElement();
    ce2.setContentElementId("foo2");
    ce2.setRole("bar");
    ce2.setType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
    ce2.setLength(23456);
    ce2.setFileName("foo.bin");

    ContentElement ce3 = new ContentElement();
    ce3.setContentElementId("foo3");
    ce3.setRole("bar");
    ce3.setType(MediaType.valueOf("image/jpeg"));
    ce3.setLength(34567);
    ce3.setFileName("foo.jpg");

    ContentElement ce4 = new ContentElement();
    ce4.setContentElementId("foo3");
    ce4.setRole("bar");
    ce4.setType(MediaType.valueOf("image/tiff"));
    ce4.setLength(34567);
    ce4.setFileName("foo.tiff");

    doc.setContentElements(Arrays.asList(ce1, ce2, ce3, ce4));

    return doc;
  }

  @Test
  public void testThat_authContextValuesAreCorrect() throws Exception {
    ArgumentCaptor<AuthorizationContext> authContextC = ArgumentCaptor.forClass(AuthorizationContext.class);
    given(authService.isAccessAllowed(any(), any(), authContextC.capture())).willReturn(true);

    documentAuthorizationService.authorizeSubResourceGet(createTestDocument(D), "metadata");

    AuthorizationContext authContext = authContextC.getValue();

    assertThat(authContext.resolveValue("document.contentElements")).isEqualTo(Boolean.TRUE);

    assertThat(authContext.resolveValue("document.contentElements.count")).isEqualTo(4);

    assertThat(authContext.resolveValue("document.contentElements.role.foo")).isEqualTo(1L);
    assertThat(authContext.resolveValue("document.contentElements.role.bar")).isEqualTo(3L);

    assertThat(authContext.resolveValue("document.contentElements.type.text/plain")).isEqualTo(1L);
    assertThat(authContext.resolveValue("document.contentElements.type.application/octet-stream")).isEqualTo(1L);
    assertThat(authContext.resolveValue("document.contentElements.type.image/jpeg")).isEqualTo(1L);

    // pattern match!
    assertThat(authContext.resolveValue("document.contentElements.type.image/*")).isEqualTo(2L);
  }
}
