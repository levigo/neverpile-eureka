package com.neverpile.eureka.search.elastic;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import com.neverpile.eureka.plugin.metadata.rest.MetadataFacet;
import com.neverpile.eureka.rest.api.document.content.ContentElementFacet;

@SpringBootTest
@ContextConfiguration(classes = ServiceConfig.class)
@Import({MetadataFacet.class, ContentElementFacet.class, AsynchronousIndexMaintenanceService.class})
@EnableAutoConfiguration
public class AsynchronousIndexMaintenanceServiceIT extends AbstractIndexMaintenanceIT {
  @Override
  public void waitOrNot() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
