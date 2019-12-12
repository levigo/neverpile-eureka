package com.neverpile.eureka.ignite;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteSpringBean;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinderAdapter;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.sharedfs.TcpDiscoverySharedFsIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.neverpile.eureka.ignite.IgniteConfigurationProperties.Finder.StaticIp;
import com.neverpile.eureka.ignite.lock.IgniteLockFactory;
import com.neverpile.eureka.ignite.queue.IgniteTaskQueue;
import com.neverpile.eureka.ignite.wal.IgniteWAL;
import com.neverpile.eureka.tasks.DistributedPersistentQueueType;
import com.neverpile.eureka.tasks.TaskQueue;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;
import com.neverpile.eureka.tx.wal.WriteAheadLog;

@Configuration
@ConditionalOnProperty(name = "neverpile-eureka.ignite.enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan
public class NeverpileIgniteAutoConfiguration {
  @Value("${neverpile-eureka.ignite.configuration.work-directory:./ignite}")
  private String workDirectory;

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

  @Autowired
  IgniteConfigurationProperties config;
  
  @Bean
  Ignite igniteInstance(final IgniteConfigurationProperties props) {
    IgniteSpringBean i = new IgniteInstanceConfiguration();

    IgniteConfiguration configuration = new IgniteConfiguration() {
      @Override
      public IgniteConfiguration setWorkDirectory(final String igniteWorkDir) {
        // ignite insists on an absolute path
        return super.setWorkDirectory(new File(igniteWorkDir).getAbsolutePath());
      }
    };
    
    i.setConfiguration(configuration);
    
    TcpCommunicationSpi communicationSpi = new TcpCommunicationSpi();
    communicationSpi.setConnectTimeout(1000);
    configuration.setCommunicationSpi(communicationSpi);
    
    TcpDiscoverySpi discovery = getDiscovery(props);
    
    configuration.setDiscoverySpi(discovery);

    if (config.getPersistence().isEnabled()) {
      DataRegionConfiguration persistent = new DataRegionConfiguration();
      persistent.setName("persistent");
      persistent.setPersistenceEnabled(true);
      persistent.setSwapPath(workDirectory + "/swap/persistent");

      DataStorageConfiguration dsc = new DataStorageConfiguration();
      dsc.setDataRegionConfigurations(persistent);

      i.configuration().setDataStorageConfiguration(dsc);
    }

    /*
     * Use the bean's own class loader for all ignite class-loading or we will get spurious
     * ClassCastExceptions when running under Spring Devtools, as the latter uses its own class
     * loader to enable hot reloads.
     */
    i.configuration().setClassLoader(getClass().getClassLoader());

    i.configuration().setIncludeEventTypes(EventType.EVT_CACHE_OBJECT_PUT, EventType.EVT_CACHE_OBJECT_REMOVED);
    
    // silence noise
    i.configuration().setMetricsLogFrequency(0);

    return i;
  }

  @ConfigurationProperties(prefix = "neverpile-eureka.ignite.discovery")
  private TcpDiscoverySpi getDiscovery(final IgniteConfigurationProperties props) {
    TcpDiscoverySpi discovery = new TcpDiscoverySpi();
    TcpDiscoveryIpFinder finder;
    switch (config.getDiscovery().getMethod()){
      default :
      case NONE :
        finder = new TcpDiscoveryNOPFinder();
        break;

      case MULTICAST :
        finder = multicastIpFinder();
        break;

      case STATIC :
        finder = staticIpFinder(props.getFinder().getStaticIp());
        break;

      case FILESYSTEM :
        finder = filesystemDiscoveryFinder();
        break;
    }
    discovery.setIpFinder(finder);
    return discovery;
  }

  @ConfigurationProperties(prefix = "neverpile-eureka.ignite.finder.filesystem")
  private TcpDiscoverySharedFsIpFinder filesystemDiscoveryFinder() {
    return new TcpDiscoverySharedFsIpFinder();
  }

  public TcpDiscoveryVmIpFinder staticIpFinder(final StaticIp staticIp) {
    TcpDiscoveryVmIpFinder tcpDiscoveryVmIpFinder = new TcpDiscoveryVmIpFinder();
    
    // bind ip addresses manually, as spring doesn't correctly bind them automatically (setter/getter names differ)
    tcpDiscoveryVmIpFinder.setAddresses(staticIp.getAddresses());
    
    return tcpDiscoveryVmIpFinder;
  }

  @ConfigurationProperties(prefix = "neverpile-eureka.ignite.finder.multicast")
  public TcpDiscoveryMulticastIpFinder multicastIpFinder() {
    return new TcpDiscoveryMulticastIpFinder();
  }

  @Bean
  @ConditionalOnProperty(name = "neverpile-eureka.ignite.wal.enabled", havingValue = "true", matchIfMissing = true)
  public WriteAheadLog igniteWAL() {
    return new IgniteWAL();
  }
  
  @Bean
  public ClusterLockFactory igniteDistributedLock() {
    return new IgniteLockFactory();
  }
  
  @Bean
  @Scope("prototype")
  public TaskQueue<?> taskQueue(final InjectionPoint ip) {
    return new IgniteTaskQueue<>(ip.getAnnotation(DistributedPersistentQueueType.class).value());
  }
}
