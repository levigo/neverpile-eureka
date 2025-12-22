package com.neverpile.eureka.search.elastic;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.neverpile.eureka.api.index.IndexMaintenanceService;
import com.neverpile.eureka.impl.index.SynchronousIndexMaintenanceBridge;

@AutoConfiguration
@ConditionalOnProperty(name = "neverpile-eureka.elastic.enabled", havingValue = "true", matchIfMissing = false)
@Import({
    ElasticsearchDocumentIndex.class, ElasticsearchIndexMaintenanceService.class, ElasticsearchQueryService.class,
    ElasticsearchIndexHealthCheck.class, NeverpileElasticsearchConfiguration.class
})
@EnableScheduling
@EnableAsync
public class NeverpileElasticsearchAutoConfiguration {
  /**
   * Provide a synchronous index maintenance bridge.
   * 
   * @return a SynchronousIndexMaintenanceBridge
   */
  @Bean
  @ConditionalOnBean(IndexMaintenanceService.class)
  public SynchronousIndexMaintenanceBridge synchronousIndexMaintenanceBridge() {
    return new SynchronousIndexMaintenanceBridge();
  }
}
