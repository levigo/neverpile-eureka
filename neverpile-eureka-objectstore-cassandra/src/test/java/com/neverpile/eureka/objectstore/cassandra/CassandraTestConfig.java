package com.neverpile.eureka.objectstore.cassandra;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;

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

  @Bean
  @Primary
  public PlatformTransactionManager transactionManager() {
    JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
    jtaTransactionManager.setUserTransaction(new UserTransaction() {
      @Override
      public void begin() throws NotSupportedException, SystemException {

      }

      @Override
      public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {

      }

      @Override
      public void rollback() throws IllegalStateException, SecurityException, SystemException {

      }

      @Override
      public void setRollbackOnly() throws IllegalStateException, SystemException {

      }

      @Override
      public int getStatus() throws SystemException {
        return 0;
      }

      @Override
      public void setTransactionTimeout(int seconds) throws SystemException {

      }
    });
    return jtaTransactionManager;
  }

  @Bean
  public TransactionWAL transactionWAL() {
    return new DefaultTransactionWAL();
  }
}
