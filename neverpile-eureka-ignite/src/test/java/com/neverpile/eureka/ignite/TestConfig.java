package com.neverpile.eureka.ignite;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;

import org.apache.ignite.IgniteSpringBean;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinderAdapter;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.neverpile.eureka.ignite.queue.IgniteTaskQueue;
import com.neverpile.eureka.tasks.DistributedPersistentQueueType;
import com.neverpile.eureka.tasks.TaskQueue;

@Configuration
@EnableTransactionManagement
@EnableAutoConfiguration(exclude = NeverpileIgniteAutoConfiguration.class)
@EnableScheduling
@Import({
    IgniteConfigurationProperties.class
})
public class TestConfig {
  private final class TcpDiscoveryNOPFinder extends TcpDiscoveryIpFinderAdapter {
    private final InetSocketAddress anyLocal = new InetSocketAddress("localhost", 0);

    @Override
    public void unregisterAddresses(final Collection<InetSocketAddress> addrs) throws IgniteSpiException {
      // nop
    }

    @Override
    public void registerAddresses(final Collection<InetSocketAddress> addrs) throws IgniteSpiException {
      // nop
    }

    @Override
    public Collection<InetSocketAddress> getRegisteredAddresses() throws IgniteSpiException {
      return Collections.singleton(anyLocal);
    }
  }

  @Bean
  IgniteSpringBean ignite() {
    IgniteInstanceConfiguration i = new IgniteInstanceConfiguration();

    i.setConfiguration(new IgniteConfiguration());

    TcpCommunicationSpi communicationSpi = new TcpCommunicationSpi();
    communicationSpi.setConnectTimeout(1000);
    communicationSpi.setSharedMemoryPort(-1);

    i.configuration().setCommunicationSpi(communicationSpi);

    TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
    discoverySpi.setIpFinder(new TcpDiscoveryNOPFinder());
    i.configuration().setDiscoverySpi(discoverySpi);

    DataRegionConfiguration persistent = new DataRegionConfiguration();
    persistent.setName("persistent");
    persistent.setPersistenceEnabled(false);
    persistent.setSwapPath("./data/ignite/swap");

    DataStorageConfiguration dsc = new DataStorageConfiguration();
    dsc.setDataRegionConfigurations(persistent);

    i.configuration().setDataStorageConfiguration(dsc);
    i.configuration().setWorkDirectory(new File("./data/ignite/work").getAbsolutePath());

    i.configuration().setIncludeEventTypes(EventType.EVT_CACHE_OBJECT_PUT, EventType.EVT_CACHE_OBJECT_REMOVED);

    return i;
  }

  @Bean
  @Scope("prototype")
  public TaskQueue<?> getQueue(final InjectionPoint ip) {
    return new IgniteTaskQueue<>(ip.getAnnotation(DistributedPersistentQueueType.class).value());
  }
}