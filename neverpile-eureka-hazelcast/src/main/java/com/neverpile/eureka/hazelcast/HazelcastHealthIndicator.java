package com.neverpile.eureka.hazelcast;

import static java.util.stream.Collectors.joining;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.hazelcast.cluster.ClusterState;
import com.hazelcast.core.HazelcastInstance;

@Component("HazelcastHealthIndicator")
public class HazelcastHealthIndicator implements HealthIndicator {
  @Autowired(required = false)
  HazelcastInstance hazelcast;

  @Autowired
  HazelcastConfigurationProperties configuration;

  public Health health() {
    Health.Builder builder = new Health.Builder();
    if (hazelcast == null) {
      builder.outOfService();
    } else if (hazelcast.getCluster().getClusterState() != ClusterState.ACTIVE) {
      builder.outOfService() //
          .withDetail("Cluster", hazelcast.getCluster().getClusterState().name());
    } else if (hazelcast.getCluster().getMembers().size() < configuration.getMinimumClusterSize()) {
      builder.outOfService() //
          .withDetail("Cluster", "Too few nodes") //
          .withDetail("Nodes", nodeNames());
    } else {
      builder.up() //
          .withDetail("Cluster", "active") //
          .withDetail("Nodes", nodeNames());
    }
    
    return builder.build();
  }

  private String nodeNames() {
    return hazelcast.getCluster().getMembers().stream().map(m -> m.toString()).collect(joining(","));
  }
}
