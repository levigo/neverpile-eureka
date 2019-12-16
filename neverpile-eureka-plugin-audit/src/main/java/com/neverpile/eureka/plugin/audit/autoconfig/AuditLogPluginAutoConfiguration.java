package com.neverpile.eureka.plugin.audit.autoconfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.plugin.audit.rest.AuditLogFacet;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;
import com.neverpile.eureka.plugin.audit.service.TimeBasedAuditIdGenerationStrategy;
import com.neverpile.eureka.plugin.audit.service.impl.DefaultAuditIdGenerationStrategy;
import com.neverpile.eureka.plugin.audit.service.impl.DefaultAuditLogService;
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

  @Bean
  @ConditionalOnBean(value = ObjectStoreService.class)
  @ConditionalOnMissingBean
  VerificationService simpleVerificationService() {
    return new DirectVerificationService();
  }

  @Bean
  @ConditionalOnBean(value = ObjectStoreService.class)
  @ConditionalOnMissingBean
  HashStrategyService simpleHashStrategyService() {
    return new HashChainService();
  }

  /**
   * Provide an implementation of {@link AuditLogService} which is based on a backing
   * {@link ObjectStoreService}. Back off if any other implementation is present.
   *
   * @return a AuditLogService implementation
   */
  @Bean
  @ConditionalOnBean(value = ObjectStoreService.class)
  @ConditionalOnMissingBean
  AuditLogService simpleAuditLogService() {
    return new DefaultAuditLogService();
  }

  @Bean
  @ConditionalOnMissingBean
  TimeBasedAuditIdGenerationStrategy timeBasedAuditIdGenerationStrategy() {
    return new DefaultAuditIdGenerationStrategy();
  }

}
