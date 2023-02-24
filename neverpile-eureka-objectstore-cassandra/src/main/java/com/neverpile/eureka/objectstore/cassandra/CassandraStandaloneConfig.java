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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.neverpile.eureka.api.ObjectStoreService;

@Configuration
@AutoConfigureBefore(value = CassandraAutoConfiguration.class,
    name = "com.neverpile.eureka.server.configuration.SimpleServiceConfiguration")
@ConditionalOnProperty(prefix = "neverpile-eureka.storage.cassandra", value = "enabled")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class CassandraStandaloneConfig extends AbstractNeverpileCassandraConfig {
  private static final Logger LOGGER = LoggerFactory.getLogger(CassandraStandaloneConfig.class);

  @Value("${neverpile-eureka.storage.cassandra.host:localhost}")
  String cassandraHost;

  @Value("${neverpile-eureka.storage.cassandra.data-center:datacenter1}")
  private String cassandraDataCenter;
  private final String keyspaceName = "objectKeySpace";

  @PostConstruct
  public void connectToCassandra() {
    LOGGER.info("-----");
    LOGGER.info("Initializing neverpile eureka - Cassandra configuration ...");
    LOGGER.info("Keyspace: '{}'", getKeyspaceName());
    LOGGER.info("Cassandra host: '{}'", cassandraHost);
    LOGGER.info("-----");

    Collection<InetSocketAddress> cassandraHostCollection = Collections.singletonList(
        new InetSocketAddress(cassandraHost, 9042));
    final CqlSession session = CqlSession.builder() //
        .addContactPoints(cassandraHostCollection) //
        .withLocalDatacenter(cassandraDataCenter) //
        .build();

    session.execute(creationQuery());
    session.execute(activationQuery());

    cassandraTemplate().createTable(true, CqlIdentifier.fromCql("object"), CassandraObject.class, new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.fromCql("objectdata"), CassandraObjectData.class,
        new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.fromCql("prefix"), CassandraObjectPrefix.class,
        new HashMap<>());
  }

  @Override
  public String getKeyspaceName() {
    return keyspaceName;
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
}
