package com.neverpile.eureka.api.documentservice;

import static org.mockito.BDDMockito.given;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

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
import com.neverpile.eureka.api.ObjectStoreService.StoreObject;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.impl.documentservice.DefaultDocumentService;
import com.neverpile.eureka.model.ObjectName;

/*
 * FIXME: there is some potential to factor this test class into a common abstract class with the
 * client tests. The latter run against a mock, static test Neverpile, though, which will support
 * far less realistic tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultDocumentServiceEventTest extends AbstractEventTest {

  @TestConfiguration
  @EnableTransactionManagement
  @EnableAutoConfiguration
  public static class ServiceConfig {
    @Bean
    DocumentService documentService() {
      return new DefaultDocumentService();
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
    given(objectStoreService.get(ObjectName.of("document", D, "document.json"))).willReturn(new StoreObject() {
      @Override
      public String getVersion() {
        return "0";
      }
  
      @Override
      public ObjectName getObjectName() {
        return ObjectName.of();
      }
  
      @Override
      public InputStream getInputStream() {
        return new ByteArrayInputStream(("{\"documentId\": \"" + D + "\"}").getBytes());
      }
    });
  }

}
