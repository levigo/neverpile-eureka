package com.neverpile.eureka.bridge.storage.cassandra;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.neverpile.eureka.api.ObjectStoreService;

@Configuration
@AutoConfigureBefore(value = CassandraAutoConfiguration.class, name = "com.neverpile.eureka.server.configuration.SimpleServiceConfiguration")
@ConditionalOnClass({Cluster.class})
@ConditionalOnExpression("${neverpile-eureka.cassandra.enabled} && ${neverpile-eureka.cassandra.embedded}")
@EnableCassandraRepositories(basePackages = "com.neverpile.eureka.bridge.storage.cassandra")
@Import({CassandraTransactionConfiguration.class, CassandraHealthIndicator.class})
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class CassandraEmbeddedConfig extends AbstractNeverpileCassandraConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(CassandraEmbeddedConfig.class);

  private static final int CASSANDRA_EMBEDDED_PORT = 9142;

  @Value("${neverpile-eureka.cassandra.startup-timeout:20000}")
  private final long startupTimeout = 20000;
  
  @PostConstruct
  public void startCassandraEmbedded() throws Exception {
    LOGGER.info("-----");
    LOGGER.info("Initializing neverpile eureka - Embedded Cassandra configuration ...");
    LOGGER.info("Keyspace: '{}'", getKeyspaceName());
    LOGGER.info("-----");
    
    String tmpDir = "target/embeddedCassandra/tmp";
    FileUtils.deleteDirectory(new File(tmpDir));
    URL resource = getClass().getClassLoader().getResource("cassandra.yaml");
    File configFile = null;
    if(null != resource) {
      configFile = new File(resource.getFile());
    }
    if (null != configFile && configFile.exists()) {
      EmbeddedCassandraServerHelper.startEmbeddedCassandra(configFile, tmpDir, startupTimeout);
    } else {
      LOGGER.error("Cassandra config file not found [cassandra.yaml]");
      EmbeddedCassandraServerHelper.startEmbeddedCassandra();
    }
    
    final Cluster cluster = Cluster.builder().addContactPoints("localhost").withPort(CASSANDRA_EMBEDDED_PORT).build();
    final Session cassandraSession = cluster.connect();
    
    cassandraSession.execute(creationQuery());
    cassandraSession.execute(activationQuery());
    
    cassandraTemplate().createTable(true, CqlIdentifier.of("object"), CassandraObject.class, new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.of("objectdata"), CassandraObjectData.class, new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.of("prefix"), CassandraObjectPrefix.class, new HashMap<>());
  }

  @PreDestroy
  public void stopCassandraEmbedded() {
    EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
  }

  @Override
  protected int getPort() {
    return CASSANDRA_EMBEDDED_PORT;
  }

  @Override
  public SchemaAction getSchemaAction(){
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
}