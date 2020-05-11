package com.neverpile.eureka.rest.api.document.core;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.common.authorization.api.AuthorizationContext;
import com.neverpile.common.authorization.api.AuthorizationService;
import com.neverpile.common.authorization.api.CoreActions;
import com.neverpile.eureka.api.BaseTestConfiguration;
import com.neverpile.eureka.api.ContentElementService;
import com.neverpile.eureka.api.DocumentAuthorizationService;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.impl.authorization.DefaultDocumentAuthorizationService;
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
public class CoreAuthContextTest extends AbstractRestAssuredTest {
  @TestConfiguration
  @Import({
      DocumentFacetAuthorizationContextContributor.class, IdFacet.class, CreationDateFacet.class,
      ModificationDateFacet.class, DefaultDocumentAuthorizationService.class
  })
  public static class ServiceConfig {
  }

  // Must mock the MultiVersioningDocumentService or we will break the app context initialization
  @MockBean
  MultiVersioningDocumentService mockDocumentService;

  @MockBean
  ContentElementService mockContentElementService;

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
    given(mockDocumentService.getDocument(any())).willAnswer(i -> {
      return Optional.of(createTestDocument(i.getArgument(0)));
    });
    given(mockDocumentService.documentExists(any())).willReturn(true);
  }

  private Document createTestDocument(final String id) {
    Document doc = new Document(id);

    doc.setDateCreated(Instant.now());
    doc.setDateModified(Instant.now());

    return doc;
  }

  @Test
  public void testThat_resourcePathIsCorrect() throws Exception {
    given(authService.isAccessAllowed(any(), any(), any())).willReturn(true);

    assertThat(documentAuthorizationService.authorizeSubResourceGet(new Document(D), "contentElements")).isTrue();

    verify(authService).isAccessAllowed(eq("document.contentElements"), eq(singleton(CoreActions.GET)), any());
  }

  @Test
  public void testThat_authContextValuesAreCorrect() throws Exception {
    ArgumentCaptor<AuthorizationContext> authContextC = ArgumentCaptor.forClass(AuthorizationContext.class);
    given(authService.isAccessAllowed(any(), any(), authContextC.capture())).willReturn(true);

    documentAuthorizationService.authorizeSubResourceGet(createTestDocument(D), "metadata");

    AuthorizationContext authContext = authContextC.getValue();

    assertThat(authContext.resolveValue("document.documentId")).isEqualTo(D);

    assertThat((Instant) authContext.resolveValue("document.dateCreated")).isCloseTo(Instant.now(), within(1000, ChronoUnit.MILLIS));
    assertThat((Instant) authContext.resolveValue("document.dateModified")).isCloseTo(Instant.now(), within(1000, ChronoUnit.MILLIS));
  }
}
