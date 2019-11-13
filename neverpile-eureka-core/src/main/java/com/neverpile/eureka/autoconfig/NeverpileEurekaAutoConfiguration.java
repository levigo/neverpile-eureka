package com.neverpile.eureka.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.annotation.RequestScope;

import com.neverpile.authorization.api.AuthorizationService;
import com.neverpile.authorization.basic.AllowAllAuthorizationService;
import com.neverpile.eureka.api.ContentElementIdGenerationStrategy;
import com.neverpile.eureka.api.ContentElementService;
import com.neverpile.eureka.api.DocumentAuthorizationService;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.api.MultiVersioningDocumentService;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.index.IndexMaintenanceService;
import com.neverpile.eureka.api.index.IndexResource;
import com.neverpile.eureka.event.EventPublisher;
import com.neverpile.eureka.event.UpdateEventAggregator;
import com.neverpile.eureka.impl.authorization.DefaultDocumentAuthorizationService;
import com.neverpile.eureka.impl.contentservice.SimpleContentElementService;
import com.neverpile.eureka.impl.documentservice.DefaultDocumentService;
import com.neverpile.eureka.impl.documentservice.DefaultMultiVersioningDocumentService;
import com.neverpile.eureka.impl.documentservice.UuidContentElementIdGenerationStrategy;
import com.neverpile.eureka.impl.documentservice.UuidDocumentIdGenerationStrategy;
import com.neverpile.eureka.impl.index.SynchronousIndexMaintenanceBridge;
import com.neverpile.eureka.rest.api.document.DocumentResource;
import com.neverpile.eureka.rest.api.document.MultiVersioningDocumentResource;
import com.neverpile.eureka.rest.api.document.content.ContentElementFacet;
import com.neverpile.eureka.rest.api.document.content.ContentElementResource;
import com.neverpile.eureka.rest.api.document.core.CreationDateFacet;
import com.neverpile.eureka.rest.api.document.core.IdFacet;
import com.neverpile.eureka.rest.api.document.core.ModificationDateFacet;
import com.neverpile.eureka.rest.api.exception.ExceptionHandlers;
import com.neverpile.eureka.rest.configuration.JacksonConfiguration;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;
import com.neverpile.eureka.tx.wal.local.FileBasedWAL;

@Configuration
@Import({
    /*
     * Due to a subtle detail of how the Spring Boot auto configuration process works, we must not
     * declare a @ComponentScan for the Jackson et. al. configuration. Instead we import
     * JacksonConfiguration as an anchor and let it do the dirty work of @ComponentScanning.
     */
    JacksonConfiguration.class, EventPublisher.class, UpdateEventAggregator.class
})
public class NeverpileEurekaAutoConfiguration {

  @ConditionalOnWebApplication
  @ConditionalOnBean(value = DocumentService.class)
  @Import({
      DocumentResource.class, CreationDateFacet.class, IdFacet.class,
      ModificationDateFacet.class, ExceptionHandlers.class, ContentElementFacet.class, ContentElementResource.class,
      IndexResource.class,
  })
  public static class RestResourceConfiguration {
    @Bean
    @ConditionalOnBean(value = MultiVersioningDocumentService.class)
    public MultiVersioningDocumentResource multiVersioningDocumentResource() {
      return new MultiVersioningDocumentResource();
    }
  }

  
  @Configuration
  @AutoConfigureBefore({RestResourceConfiguration.class})
  public static class CoreServiceConfiguration {
    /**
     * Provide an implementation of {@link DocumentService} which is based on a backing
     * {@link ObjectStoreService}. Back off if any other implementation is present. We expose the
     * bean as a SimpleDocumentService since the latter implements both DocumentService and
     * DocumentAssociatedEntityStore.
     * <p>
     * The default is to provide an implementation which is based on multi-versioning, and treats
     * the object store as append-only.
     *
     * @return a MultiVersioningDocumentService implementation
     */
    @Bean
    @ConditionalOnBean(ObjectStoreService.class)
    @ConditionalOnMissingBean(DocumentService.class)
    @ConditionalOnProperty(name = "neverpile-eureka.document-service.enable-multi-versioning", matchIfMissing = true)
    public DefaultMultiVersioningDocumentService defaultMultiVersioningDocumentService() {
      return new DefaultMultiVersioningDocumentService();
    }

    /**
     * Provide an implementation of {@link DocumentService} which is based on a backing
     * {@link ObjectStoreService}. Back off if any other implementation is present. We expose the
     * bean as a SimpleDocumentService since the latter implements both DocumentService and
     * DocumentAssociatedEntityStore.
     * <p>
     * This the non multi-versioning implementation is activated by setting the property
     * <code>neverpile-eureka.document-service.enable-multi-versioning=false</code>.
     *
     * @return a DocumentService implementation
     */
    @Bean
    @ConditionalOnBean(ObjectStoreService.class)
    @ConditionalOnMissingBean(DocumentService.class)
    @ConditionalOnProperty(name = "neverpile-eureka.document-service.enable-multi-versioning", matchIfMissing = false)
    public DefaultDocumentService defaultDocumentService() {
      return new DefaultDocumentService();
    }
  }

  /**
   * Provide an implementation of {@link DocumentIdGenerationStrategy} which is based on UUIDs.
   * <p>
   * Back off if any other implementation is present.
   *
   * @return a DocumentIdGenerationStrategy implementation
   */
  @Bean
  @ConditionalOnMissingBean
  public DocumentIdGenerationStrategy documentIdGenerationStrategy() {
    return new UuidDocumentIdGenerationStrategy();
  }

  /**
   * Provide an implementation of {@link ContentElementIdGenerationStrategy} which is based on
   * UUIDs.
   * <p>
   * Back off if any other implementation is present.
   * 
   * @return a ContentElementIdGenerationStrategy implementation
   */
  @Bean
  @ConditionalOnMissingBean
  public ContentElementIdGenerationStrategy contentElementIdGenerationStrategy() {
    return new UuidContentElementIdGenerationStrategy();
  }

  /**
   * Provide a default implementation of {@link TransactionWAL} which is based on a backing
   * {@link WriteAheadLog}.
   * <p>
   * Back off if any other implementation is present.
   *
   * @return a TransactionWAL implementation
   */
  @Bean
  @RequestScope
  @ConditionalOnMissingBean
  public TransactionWAL defaultTransactionWAL() {
    return new DefaultTransactionWAL();
  }

  /**
   * Provide an implementation of {@link WriteAheadLog} which is backed by a local file. This
   * implementation is <em>not</em> suitable for distributed installations.
   * <p>
   * Back off if any other implementation is present.
   *
   * @return a WriteAheadLog implementation
   */
  @Bean
  @ConditionalOnMissingBean
  public WriteAheadLog fileBasedWriteAheadLog() {
    return new FileBasedWAL();
  }

  /**
   * Provide an implementation of {@link AuthorizationService} that allows all accesses.
   * Authorization must be implemented using other methods.
   * <p>
   * Back off if any other implementation is present.
   *
   * @return an AuthorizationService implementation
   */
  @Bean
  @ConditionalOnMissingBean
  public AuthorizationService allowAllAuthorizationService() {
    return new AllowAllAuthorizationService();
  }

  /**
   * Provide an implementation of {@link DocumentAuthorizationService} which delegates to an
   * {@link AuthorizationService}.
   * <p>
   * Back off if any other implementation is present.
   *
   * @return a DocumentAuthorizationService implementation
   */
  @Bean
  @ConditionalOnMissingBean
  public DocumentAuthorizationService documentAuthorizationService() {
    return new DefaultDocumentAuthorizationService();
  }

  /**
   * Provide an implementation of {@link ContentElementService} which is based on a backing
   * {@link ObjectStoreService}.
   * <p>
   * Back off if any other implementation is present.
   *
   * @return a ContentElementService implementation
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(ObjectStoreService.class)
  public ContentElementService contentElementService() {
    return new SimpleContentElementService();
  }
  
  /**
   * Provide a synchronous index maintenance bridge.
   * 
   * FIXME: how do we back off if the application wants to handle index maintenance differently?
   * 
   * @return a SynchronousIndexMaintenanceBridge
   */
  @Bean
  @ConditionalOnBean(IndexMaintenanceService.class) 
  public SynchronousIndexMaintenanceBridge synchronousIndexMaintenanceBridge() {
    return new SynchronousIndexMaintenanceBridge();
  }
}
