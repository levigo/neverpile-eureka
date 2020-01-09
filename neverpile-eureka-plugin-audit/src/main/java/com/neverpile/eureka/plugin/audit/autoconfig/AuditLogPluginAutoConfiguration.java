package com.neverpile.eureka.plugin.audit.autoconfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.neverpile.common.openapi.ResourceOpenApiFragment;
import com.neverpile.common.openapi.OpenApiFragment;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.plugin.audit.rest.AuditLogFacet;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;
import com.neverpile.eureka.plugin.audit.service.TimeBasedAuditIdGenerationStrategy;
import com.neverpile.eureka.plugin.audit.service.impl.DefaultAuditIdGenerationStrategy;
import com.neverpile.eureka.plugin.audit.service.impl.DefaultAuditLogService;
import com.neverpile.eureka.plugin.audit.storage.AuditObjectStoreBridge;
import com.neverpile.eureka.plugin.audit.storage.AuditStorageBridge;
import com.neverpile.eureka.plugin.audit.verification.HashStrategyService;
import com.neverpile.eureka.plugin.audit.verification.VerificationService;
import com.neverpile.eureka.plugin.audit.verification.hashchain.HashChainService;
import com.neverpile.eureka.plugin.audit.verification.impl.DirectVerificationService;

/**
 * Spring-Boot Auto-configuration for the neverpile eureka audit log plugin.
 */
@Configuration
@ConditionalOnClass({
    Document.class
})
@ComponentScan(basePackageClasses = {
    AuditLogFacet.class
})
public class AuditLogPluginAutoConfiguration {

  /**
   * Provide an implementation of {@link AuditStorageBridge}.
   * Back off if any other implementation is present.
   *
   * @return
   */
  @Bean
  @ConditionalOnBean(value = ObjectStoreService.class)
  @ConditionalOnMissingBean
  AuditStorageBridge auditStorageBridge() {
    return new AuditObjectStoreBridge();
  }

  /**
   * Provide an implementation of {@link VerificationService}.
   * Back off if any other implementation is present.
   *
   * @return
   */
  @Bean
  @ConditionalOnMissingBean
  VerificationService simpleVerificationService() {
    return new DirectVerificationService();
  }

  /**
   * Provide an implementation of {@link HashStrategyService}.
   * Back off if any other implementation is present.
   *
   * @return
   */
  @Bean
  @ConditionalOnBean(value = AuditStorageBridge.class)
  @ConditionalOnMissingBean
  HashStrategyService simpleHashStrategyService() {
    return new HashChainService();
  }

  /**
   * Provide an implementation of {@link AuditLogService}.
   * Back off if any other implementation is present.
   *
   * @return a AuditLogService implementation
   */
  @Bean
  @ConditionalOnBean(value = AuditStorageBridge.class)
  @ConditionalOnMissingBean
  AuditLogService simpleAuditLogService() {
    return new DefaultAuditLogService();
  }

  /**
   * Provide an implementation of {@link TimeBasedAuditIdGenerationStrategy}.
   * Back off if any other implementation is present.
   *
   * @return
   */
  @Bean
  @ConditionalOnMissingBean
  TimeBasedAuditIdGenerationStrategy timeBasedAuditIdGenerationStrategy() {
    return new DefaultAuditIdGenerationStrategy();
  }

  
  @Bean
  public OpenApiFragment auditLogOpenApiFragment() {
    return new ResourceOpenApiFragment("eureka", "audit", new ClassPathResource("com/neverpile/eureka/plugin/audit/openapi.yaml"));
  }
}
