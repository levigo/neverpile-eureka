package com.neverpile.eureka.search.elastic;

import java.time.Instant;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.neverpile.eureka.rest.api.document.DocumentFacet;
import com.neverpile.eureka.rest.api.document.core.CreationDateFacet;
import com.neverpile.eureka.rest.api.document.core.IdFacet;
import com.neverpile.eureka.rest.api.document.core.ModificationDateFacet;
import com.neverpile.eureka.tasks.TaskQueue;

@Configuration
public class ServiceConfig {
  @Value("${elastic.host:localhost}")
  String elasticHost;

  @Value("${elastic.port:9200}")
  int elasticPort;


  @Bean
  DocumentFacet<String> getIdFacet() {
    return new IdFacet();
  }

  @Bean
  DocumentFacet<Instant> getCreationDateFacet() {
    return new CreationDateFacet();
  }

  @Bean
  DocumentFacet<Instant> getModificationDateFacet() {
    return new ModificationDateFacet();
  }

  @Bean(destroyMethod = "close")
  public RestHighLevelClient testElasticClient() {
    return new RestHighLevelClient(RestClient.builder(new HttpHost(elasticHost, elasticPort, "http")));
  }

  @Bean
  ElasticsearchDocumentIndex getElasticsearchDocumentIndex() {
    ElasticsearchDocumentIndex.INDEX_ALIAS_READ = "test-document-read";
    ElasticsearchDocumentIndex.INDEX_ALIAS_WRITE = "test-document-write";
    ElasticsearchDocumentIndex index = new ElasticsearchDocumentIndex();
    return index;
  }

  @Bean
  TaskQueue<?> distributedPersistentQueue() {
    return new MockTaskQueue<>();
  }
}