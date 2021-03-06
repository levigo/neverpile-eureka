package com.neverpile.eureka.objectstore.cassandra;

import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

@Configuration
@AutoConfigureBefore(value = CassandraAutoConfiguration.class,
    name = "com.neverpile.eureka.server.configuration.SimpleServiceConfiguration")
@EnableCassandraRepositories(basePackages = "com.neverpile.eureka.bridge.storage.cassandra")
@Import(CassandraTransactionConfiguration.class)
@EnableAutoConfiguration
public class CassandraTestConfig extends AbstractNeverpileCassandraConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(CassandraTestConfig.class);

  @Value("${cassandra.host:localhost}")
  String cassandraHost;

  int cassandraPort = 9042;

  @PostConstruct
  public void connectToCassandra() throws Exception {
    LOGGER.info("-----");
    LOGGER.info("Initializing neverpile eureka - Cassandra configuration ...");
    LOGGER.info("Keyspace: '{}'", getKeyspaceName());
    LOGGER.info("Cassandra host: '{}'", cassandraHost);
    LOGGER.info("-----");

    final Cluster cluster = Cluster.builder().addContactPoints(cassandraHost).withPort(
        cassandraPort).build();

    final Session cassandraSession = cluster.connect();
    cassandraSession.execute(creationQuery());
    cassandraSession.execute(activationQuery());

    cassandraTemplate().createTable(true, CqlIdentifier.of("object"), CassandraObject.class, new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.of("objectdata"), CassandraObjectData.class, new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.of("prefix"), CassandraObjectPrefix.class, new HashMap<>());
  }

  @Override
  protected String getContactPoints() {
    return cassandraHost;
  }
}