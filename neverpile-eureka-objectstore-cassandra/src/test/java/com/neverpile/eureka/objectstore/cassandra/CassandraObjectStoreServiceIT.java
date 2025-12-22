package com.neverpile.eureka.objectstore.cassandra;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.cassandra.autoconfigure.DataCassandraAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.neverpile.eureka.api.objectstore.AbstractObjectStoreServiceTest;

@SpringBootTest(classes = CassandraTestConfig.class)
@EnableAutoConfiguration(exclude = {
    DataCassandraAutoConfiguration.class
})
@TestPropertySource(properties = {"spring.cassandra.jmx-enabled=false"})
public class CassandraObjectStoreServiceIT extends AbstractObjectStoreServiceTest {
  @Autowired
  private CassandraObjectStoreService cassandraObjectStore;

  @PostConstruct
  public void cleanUp() {
    cassandraObjectStore.truncateDataTable();
    cassandraObjectStore.truncateObjectTable();
    cassandraObjectStore.truncatePrefixTable();
  }
}
