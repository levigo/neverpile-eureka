package com.neverpile.eureka.search.elastic;

import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.eureka.plugin.metadata.rest.MetadataFacet;
import com.neverpile.eureka.rest.api.document.content.ContentElementFacet;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = ServiceConfig.class)
@Import({MetadataFacet.class, ContentElementFacet.class, ElasticsearchIndexMaintenanceService.class})
@EnableAutoConfiguration
public class ElasticsearchIndexMaintenanceIT extends AbstractIndexMaintenanceIT {
  @Override
  public void waitOrNot() {
    // don't wait.
  }
}