package com.neverpile.eureka.objectstore.cassandra;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.SessionFactory;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeState;

@Component("CassandraHealthIndicator")
@ConditionalOnProperty(prefix = "neverpile-eureka.storage.cassandra", value = "enabled")
public class CassandraHealthIndicator implements HealthIndicator {

  @Autowired(required = false)
  private SessionFactory sessionFactory;

  @Autowired(required = false)
  private CassandraOperations operations;

  public Health health() {
    Health.Builder builder = new Health.Builder();
    if (operations == null) {
      builder.outOfService();
    } else {
      this.doHealthCheck(builder);
    }
    return builder.build();
  }


  private void doHealthCheck(final Health.Builder builder) {
    this.checkSessionState();
    CqlSession session = this.sessionFactory.getSession();

    Collection<Node> nodes = session.getMetadata().getNodes().values();
    List<Node> nodeUp = nodes.stream().filter((node) -> node.getState() == NodeState.UP).collect(Collectors.toList());

    if (!nodeUp.isEmpty()) {
      builder.withDetail("ConnectionHost Size", nodes.size());
      nodeUp.forEach(f -> {
        builder.withDetail("Host ID", f.getHostId());
        builder.withDetail("BroadcastAddress", f.getBroadcastAddress());
        builder.withDetail("Datacenter", f.getDatacenter());
        builder.withDetail("Cassandra Version", f.getCassandraVersion().toString());
        builder.withDetail("Open Connections", f.getOpenConnections());
        builder.up();
      });
    } else {
      builder.down();
    }
  }

  private boolean checkSessionState() {
    try {
      this.operations.getCqlOperations().execute("SELECT now() FROM system.local");
      return true;
    } catch (DataAccessException ex) {
      return false;
    }
  }
}