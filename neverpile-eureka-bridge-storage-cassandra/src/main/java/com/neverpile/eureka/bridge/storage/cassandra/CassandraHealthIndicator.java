package com.neverpile.eureka.bridge.storage.cassandra;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.SessionFactory;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metrics.DefaultSessionMetric;

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
    this.checkSessionState(builder);
    
    CqlSession session = this.sessionFactory.getSession();

    session.getMetrics().ifPresent(m -> {
      builder.withDetail("ConnectionHost Size", session.getMetrics().get().getSessionMetric(DefaultSessionMetric.CONNECTED_NODES));
    });
    
    return builder;
  }

  private void checkSessionState(final Health.Builder builder) {
    try {
      this.operations.getCqlOperations().execute("SELECT now() FROM system.local");
      builder.up();
    } catch (DataAccessException ex) {
      builder.down(ex);
    }
  }
}