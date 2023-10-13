package com.neverpile.eureka.objectstore.cassandra;

import jakarta.annotation.PostConstruct;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.eureka.api.objectstore.AbstractObjectStoreServiceTest;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CassandraTestConfig.class)
@EnableAutoConfiguration(exclude = {
    CassandraDataAutoConfiguration.class
})
@TestPropertySource(properties = {"spring.data.cassandra.jmx-enabled=false"})
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
