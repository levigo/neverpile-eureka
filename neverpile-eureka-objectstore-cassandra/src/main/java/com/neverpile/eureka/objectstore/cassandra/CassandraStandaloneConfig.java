package com.neverpile.eureka.objectstore.cassandra;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import com.datastax.oss.driver.api.core.CqlSession;

@Configuration
@AutoConfigureBefore(value = CassandraAutoConfiguration.class, name = "com.neverpile.eureka.server.configuration.SimpleServiceConfiguration")
@ConditionalOnExpression("${neverpile-eureka.cassandra.enabled}")
@EnableCassandraRepositories(basePackages = "com.neverpile.eureka.objectstore.cassandra")
@Import(CassandraTransactionConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class CassandraStandaloneConfig extends AbstractNeverpileCassandraConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(CassandraStandaloneConfig.class);

  @Value("${neverpile-eureka.cassandra.host:localhost}")
  String cassandraHost;

  @PostConstruct
  public void connectToCassandra() throws Exception {
    LOGGER.info("-----");
    LOGGER.info("Initializing neverpile eureka - Cassandra configuration ...");
    LOGGER.info("Keyspace: '{}'", getKeyspaceName());
    LOGGER.info("Cassandra host: '{}'", cassandraHost);
    LOGGER.info("-----");

    Collection<InetSocketAddress> cassandraHostCollection = Collections.singletonList(new InetSocketAddress(cassandraHost, 9042));
    final CqlSession session = CqlSession.builder().addContactPoints(cassandraHostCollection).build();

    session.execute(creationQuery());
    session.execute(activationQuery());
    
    cassandraTemplate().createTable(true, CqlIdentifier.fromCql("object"), CassandraObject.class, new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.fromCql("objectdata"), CassandraObjectData.class, new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.fromCql("prefix"), CassandraObjectPrefix.class, new HashMap<>());
  }
  
  @Override
  protected String getContactPoints() {
    return cassandraHost;
  }
}