package com.neverpile.eureka.bridge.storage.cassandra;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.SessionFactory;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

import com.datastax.driver.core.Session;

@Component("CassandraHealthIndicator")
@ConditionalOnExpression("${neverpile-eureka.cassandra.enabled}")
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
      builder = this.doHealthCheck(builder);
    }
    return builder.build();
  }


  private Health.Builder doHealthCheck(final Health.Builder builder) {
    this.checkSessionState();
    Session.State state = this.sessionFactory.getSession().getState();

    builder.withDetail("ConnectionHost Size", state.getConnectedHosts().size());
    state.getConnectedHosts().forEach(f -> {
      builder.withDetail("Host ID", f.getHostId());
      builder.withDetail("BroadcastAddress", f.getBroadcastAddress());
      builder.withDetail("Datacenter", f.getDatacenter());
      builder.withDetail("Host Address", f.getAddress());
      builder.withDetail("Cassandra Version", f.getCassandraVersion().toString());
      builder.withDetail("Open Queries", state.getInFlightQueries(f));
      builder.withDetail("Open Connections", state.getOpenConnections(f));
      builder.withDetail("Trashed Connections", state.getTrashedConnections(f));

      if (!f.getState().equals(Status.UP.getCode()) && checkSessionState()) {
        builder.up();
      } else {
        builder.down();
      }
    });
    return builder;
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