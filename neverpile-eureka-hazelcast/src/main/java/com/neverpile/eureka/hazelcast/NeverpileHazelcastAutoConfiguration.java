package com.neverpile.eureka.hazelcast;

import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MultiMapConfig;
import com.neverpile.eureka.hazelcast.lock.HazelcastSimpleLockFactory;
import com.neverpile.eureka.hazelcast.queue.HazelcastTaskQueue;
import com.neverpile.eureka.hazelcast.reference.HazelcastReference;
import com.neverpile.eureka.hazelcast.wal.HazelcastWAL;
import com.neverpile.eureka.tasks.DistributedPersistentQueueType;
import com.neverpile.eureka.tasks.TaskQueue;
import com.neverpile.eureka.tx.atomic.DistributedAtomicReference;
import com.neverpile.eureka.tx.atomic.DistributedAtomicType;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;
import com.neverpile.eureka.tx.wal.WriteAheadLog;

@AutoConfiguration
@ConditionalOnProperty(name = "neverpile-eureka.hazelcast.enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan
public class NeverpileHazelcastAutoConfiguration {
  @Autowired
  HazelcastConfigurationProperties hazelcastConfig;

  @Bean
  @ConditionalOnProperty(name = "neverpile-eureka.hazelcast.wal.enabled", havingValue = "true", matchIfMissing = true)
  WriteAheadLog hazelcastWAL() {
    return new HazelcastWAL();
  }

  @Bean
  @Scope("prototype")
  @ConditionalOnProperty(name = "neverpile-eureka.hazelcast.enabled", havingValue = "true", matchIfMissing = false)
  public TaskQueue<?> getQueue(final InjectionPoint ip) {
    return new HazelcastTaskQueue<>(ip.getAnnotation(DistributedPersistentQueueType.class).value());
  }

  @Bean
  @Scope("prototype")
  @ConditionalOnProperty(name = "neverpile-eureka.hazelcast.enabled", havingValue = "true", matchIfMissing = false)
  public DistributedAtomicReference<?> hazelcastDistributedReference(final InjectionPoint ip) {
    return new HazelcastReference<>(ip.getAnnotation(DistributedAtomicType.class).value());
  }

  @Bean
  @ConditionalOnProperty(name = "neverpile-eureka.hazelcast.enabled", havingValue = "true", matchIfMissing = false)
  public ClusterLockFactory hazelcastDistributedLock() {
    return new HazelcastSimpleLockFactory();
  }

  @Bean
  public Config config() {
    Config configuration = hazelcastConfig.getConfiguration();

    configuration.addMapConfig(new MapConfig() //
        .setName(HazelcastWAL.class.getName() + ".*") //
        .setBackupCount(2));
    
    configuration.addMultiMapConfig(new MultiMapConfig() //
        .setName(HazelcastTaskQueue.class.getName() + ".*") //
        .setBackupCount(2));

    return configuration;
  }
}
