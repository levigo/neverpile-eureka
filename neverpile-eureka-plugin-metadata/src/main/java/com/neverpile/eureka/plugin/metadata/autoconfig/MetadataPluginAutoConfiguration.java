package com.neverpile.eureka.plugin.metadata.autoconfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.neverpile.common.openapi.DefaultOpenApiFragment;
import com.neverpile.common.openapi.OpenApiFragment;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.plugin.metadata.rest.MetadataFacet;
import com.neverpile.eureka.plugin.metadata.service.MetadataService;
import com.neverpile.eureka.plugin.metadata.service.impl.SimpleMetadataService;

/**
 * Spring-Boot Auto-configuration for the neverpile eureka metadata plugin.
 */
@Configuration
@ConditionalOnClass({
    Document.class
})
@ComponentScan(basePackageClasses = {
    MetadataFacet.class
})
public class MetadataPluginAutoConfiguration {

  /**
   * Provide an implementation of {@link MetadataService} which is based on a backing
   * {@link ObjectStoreService}. Back off if any other implementation is present.
   * 
   * @return a MetadataService implementation
   */
  @Bean
  @ConditionalOnBean(value = ObjectStoreService.class)
  @ConditionalOnMissingBean
  MetadataService simpleMetadataService() {
    return new SimpleMetadataService();
  }
  
  @Bean
  public OpenApiFragment metadataOpenApiFragment() {
    return new DefaultOpenApiFragment("eureka", "metadata", new ClassPathResource("com/neverpile/eureka/plugin/metadata/openapi.yaml"));
  }
}
