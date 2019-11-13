package com.neverpile.eureka.search.elastic;

import java.io.IOException;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("ElasticIndexHealthIndicator")
public class ElasticsearchIndexHealthCheck implements HealthIndicator {
  protected static final Logger logger = LoggerFactory.getLogger(ElasticsearchIndexHealthCheck.class);

  @Autowired(required = false)
  RestHighLevelClient client;

  public Health health() {
    Health.Builder builder = new Health.Builder();
    if (client == null) {
      builder.outOfService();
    } else {
      builder.withDetail("Indexname", "Elastic Search");
      builder = this.checkSessionState(builder);
    }
    return builder.build();
  }

  private Health.Builder checkSessionState(final Health.Builder builder) {
    try {
      Response response = client.getLowLevelClient().performRequest(new Request("GET", "_cluster/health"));
      builder.withDetail("Host", response.getHost().toString());
      return builder.up();
    } catch (IOException e1) {
      logger.error("ConnectException - Can not connect to elasticsearch");
      return builder.down();
    }
  }
}
