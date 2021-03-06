package com.neverpile.eureka.objectstore.cassandra;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;

import com.neverpile.eureka.api.ObjectStoreService;

public abstract class AbstractNeverpileCassandraConfig extends AbstractCassandraConfiguration {

  public enum ReplicationStrategy {
    Simple, NetworkTopology
  }

  private String keyspaceName = "objectKeySpace";

  private ReplicationStrategy replicationStrategy = ReplicationStrategy.Simple;

  private int replicationFactor = 1;

  public AbstractNeverpileCassandraConfig() {
    super();
  }

  protected String activationQuery() {
    return "USE " + getKeyspaceName() + ";";
  }

  protected String creationQuery() {
    return "CREATE KEYSPACE IF NOT EXISTS " + getKeyspaceName() + " WITH replication = { 'class': '"
        + getReplicationStrategy() + "Strategy', " //
        + "'replication_factor': '" + getReplicationFactor() + "' };";
  }

  @Override
  public String getKeyspaceName() {
    return keyspaceName;
  }

  @Override
  public SchemaAction getSchemaAction() {
    return SchemaAction.CREATE_IF_NOT_EXISTS;
  }

  @Bean
  @Primary
  public ObjectStoreService cassandraObjectStoreService() {
    return new CassandraObjectStoreService();
  }
//
//  @Bean
//  public CassandraMappingContext cassandraMapping() {
//    return new BasicCassandraMappingContext();
//  }

  @Value("${neverpile-eureka.cassandra.keyspace-name:objectKeySpace}")
  protected void setKeyspaceName(final String keyspaceName) {
    this.keyspaceName = keyspaceName;
  }

  int getReplicationFactor() {
    return replicationFactor;
  }

  @Value("${neverpile-eureka.cassandra.replication-factor:1}")
  void setReplicationFactor(final int replicationFactor) {
    this.replicationFactor = replicationFactor;
  }

  ReplicationStrategy getReplicationStrategy() {
    return replicationStrategy;
  }

  @Value("${neverpile-eureka.cassandra.replication-strategy:Simple}")
  void setReplicationStrategy(final ReplicationStrategy replicationStrategy) {
    this.replicationStrategy = replicationStrategy;
  }

}