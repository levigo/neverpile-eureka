package com.neverpile.eureka.ignite;

import static java.util.stream.Collectors.*;

import org.apache.ignite.Ignite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("IgniteHealthIndicator")
public class IgniteHealthIndicator implements HealthIndicator {
  @Autowired(required = false)
  Ignite ignite;

  @Value("${neverpile-eureka.ignite.minimum-cluster-size:2}")
  int minimumClusterSize;

  public Health health() {
    Health.Builder builder = new Health.Builder();
    if (ignite == null) {
      builder.outOfService();
    } else if (!ignite.cluster().active()) {
      builder.outOfService() //
          .withDetail("Cluster", "inactive");
    } else if (ignite.cluster().nodes().size() < minimumClusterSize) {
      builder.outOfService() //
          .withDetail("Cluster", "Too few nodes") //
          .withDetail("Nodes", ignite.cluster().hostNames().stream().collect(joining(",")));
    } else {
      builder.up() //
          .withDetail("Cluster", "active") //
          .withDetail("Nodes", ignite.cluster().hostNames().stream().collect(joining(",")));
    }
    return builder.build();
  }
}
