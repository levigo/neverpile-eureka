package com.neverpile.eureka.bridge.storage.cassandra;

import java.io.File;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.cassandra.io.util.FileUtils;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.CqlIdentifier;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.impl.tx.lock.LocalLockFactory;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;
import com.neverpile.eureka.tx.wal.local.FileBasedWAL;

@Configuration
@EnableCassandraRepositories(basePackages = "com.neverpile.eureka.bridge.storage.cassandra")
@Import(CassandraTransactionConfiguration.class)
public class CassandraTestConfig extends AbstractNeverpileCassandraConfig {

  @PostConstruct
  public void startCassandraEmbedded() throws Exception {
    String tmpDir = "target/embeddedCassandra/tmp";
    FileUtils.deleteRecursive(new File(tmpDir));
    File configFile = new File(getClass().getClassLoader().getResource("cassandra.yaml").getFile());
    if (configFile.exists()) {
      EmbeddedCassandraServerHelper.startEmbeddedCassandra(configFile, tmpDir, 10000);
    } else {
      System.err.println("Cassandra config file not found [cassandra.yaml]");
      EmbeddedCassandraServerHelper.startEmbeddedCassandra();
    }

    final Cluster cluster = Cluster.builder().addContactPoints("127.0.0.1").withPort(9142)//
        /*
         * Disable metrics due to an incompatibility between spring's dropwizard 4.0.3 vs.
         * cassandra's 3.2.2. See
         * https://stackoverflow.com/questions/53101753/spring-boot-data-cassandra-reactive-
         * jmxreporter-problem for details.
         */
        .withoutMetrics()//
        .build();
    final Session cassandraSession = cluster.connect();
    cassandraSession.execute("CREATE KEYSPACE IF NOT EXISTS "
        + "testObjectKeySpace WITH replication = { 'class': 'SimpleStrategy', 'replication_factor': '1' };");
    cassandraSession.execute("USE testObjectKeySpace;");
    cassandraTemplate().createTable(true, CqlIdentifier.of("object"), CassandraObject.class, new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.of("objectdata"), CassandraObjectData.class, new HashMap<>());
    cassandraTemplate().createTable(true, CqlIdentifier.of("prefix"), CassandraObjectPrefix.class, new HashMap<>());
  }

  /**
   * Disable metrics due to an incompatibility between spring's dropwizard 4.0.3 vs. cassandra's
   * 3.2.2. See <a href=
   * "https://stackoverflow.com/questions/53101753/spring-boot-data-cassandra-reactive-jmxreporter-problem">this</a>
   * for details.
   */
  @Override
  protected boolean getMetricsEnabled() {
    return false;
  }

  @PreDestroy
  public void stopCassandraEmbedded() {
    EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
  }

  @Override
  protected int getPort() {
    return 9142;
  }

  @Override
  public SchemaAction getSchemaAction() {
    return SchemaAction.CREATE_IF_NOT_EXISTS;
  }

  @Override
  public String getKeyspaceName() {
    return "testObjectKeySpace";
  }

  @Bean
  @Primary
  @Override
  public ObjectStoreService cassandraObjectStoreService() {
    return new CassandraObjectStoreService();
  }

  @Bean
  WriteAheadLog fileBasedWal() {
    return new FileBasedWAL();
  }

  @Bean
  TransactionWAL wal() {
    return new DefaultTransactionWAL();
  }
  
  @Bean
  ClusterLockFactory lock() {
    return new LocalLockFactory();
  }
}