package com.neverpile.eureka.objectstore.cassandra;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.neverpile.eureka.api.ObjectStoreService;

@Configuration
@AutoConfigureBefore(value = {CassandraAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class},
    name = "com.neverpile.eureka.server.configuration.SimpleServiceConfiguration")
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

    Collection<InetSocketAddress> cassandraHostCollection = Collections.singletonList(
        new InetSocketAddress(cassandraHost, cassandraPort));

    final CqlSession session = CqlSession.builder().withLocalDatacenter("datacenter1").addContactPoints(
        cassandraHostCollection).build();

    session.execute(creationQuery());
    session.execute(activationQuery());

    cassandraTemplate().createTable(true, CqlIdentifier.fromCql("object"), CassandraObject.class, new HashMap<>());
    Optional<TableMetadata> m = cassandraTemplate().getTableMetadata(CqlIdentifier.fromCql(getKeyspaceName()),
        CqlIdentifier.fromCql("object"));

    cassandraTemplate().createTable(true, CqlIdentifier.fromCql("objectdata"), CassandraObjectData.class,
        new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.fromCql("prefix"), CassandraObjectPrefix.class,
        new HashMap<>());
  }

  @Override
  protected String getContactPoints() {
    return cassandraHost;
  }

  @Bean
  @Primary
  public ObjectStoreService cassandraObjectStoreService() {
    return new CassandraObjectStoreService();
  }

  @Primary
  @Bean
  public PlatformTransactionManager transactionManager() {
    return new PlatformTransactionManager() {
      @Override
      public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        return null;
      }

      @Override
      public void commit(TransactionStatus status) throws TransactionException {

      }

      @Override
      public void rollback(TransactionStatus status) throws TransactionException {

      }
    };
  }

  @Bean
  public TransactionTemplate transactionTemplate() {
    return new TransactionTemplate(transactionManager());
  }
}
