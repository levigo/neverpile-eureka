package com.neverpile.eureka.search.elastic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(name = "neverpile-eureka.elastic.enabled", havingValue = "true", matchIfMissing = false)
@Import({ElasticsearchDocumentIndex.class, ElasticsearchIndexMaintenanceService.class, ElasticsearchQueryService.class,
    ElasticsearchIndexHealthCheck.class
})
public class NeverpileElasticsearchAutoConfiguration {

}
