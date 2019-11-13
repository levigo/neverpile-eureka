package com.neverpile.eureka.hazelcast;

import org.springframework.beans.factory.InjectionPoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.hazelcast.config.Config;
import com.neverpile.eureka.hazelcast.queue.HazelcastTaskQueue;
import com.neverpile.eureka.tasks.DistributedPersistentQueueType;
import com.neverpile.eureka.tasks.TaskQueue;

@Configuration
@EnableTransactionManagement
@EnableAutoConfiguration
@EnableScheduling
@Import({HazelcastConfigurationProperties.class})
public class TestConfig {
  @Bean
  Config hazelcastConfig() {
    Config config = new Config();
    
    // disable discovery
    config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
    config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);

    return config;
  }
  
  @Bean
  @Scope("prototype")
  public TaskQueue<?> getQueue(final InjectionPoint ip) {
    return new HazelcastTaskQueue<>(ip.getAnnotation(DistributedPersistentQueueType.class).value());
  }
}