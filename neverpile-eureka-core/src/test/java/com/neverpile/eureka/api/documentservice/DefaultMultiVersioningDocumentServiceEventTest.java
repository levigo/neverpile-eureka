package com.neverpile.eureka.api.documentservice;

import static com.neverpile.eureka.impl.documentservice.DefaultMultiVersioningDocumentService.DOCUMENT_PREFIX;
import static com.neverpile.eureka.impl.documentservice.DefaultMultiVersioningDocumentService.VERSION_FORMATTER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.impl.documentservice.DefaultMultiVersioningDocumentService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.model.ObjectName;

/*
 * FIXME: there is some potential to factor this test class into a common abstract class with the
 * client tests. The latter run against a mock, static test Neverpile, though, which will support
 * far less realistic tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultMultiVersioningDocumentServiceEventTest extends AbstractEventTest {

  @TestConfiguration
  @EnableTransactionManagement
  @EnableAutoConfiguration
  public static class ServiceConfig {
    @Bean
    DocumentService documentService() {
      return new DefaultMultiVersioningDocumentService();
    }

    @Bean
    @Primary
    EventPublisher eventPublisher(final ApplicationEventPublisher aep) {
      return new EventPublisher(aep);
    }

    @Bean
    EventCounter mockEventReceiver() {
      return new EventCounter();
    }
  }

  protected void mockExistingDocument() {
    Document doc = new Document(D);
    doc.setVersionTimestamp(Instant.now());
    
    ObjectName metaPrefix = ObjectName.of(DOCUMENT_PREFIX, D, "meta");
    ObjectName name = metaPrefix.append(VERSION_FORMATTER.format(doc.getVersionTimestamp()));

    given(objectStoreService.list(eq(metaPrefix))).will(i -> Stream.of(new DocObject(mapper, doc, name)));
    given(objectStoreService.get(eq(name))).will(i -> new DocObject(mapper, doc, name));
  }

}
