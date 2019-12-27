package com.neverpile.eureka.ignite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ignite.configuration.CacheConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;

/**
 * Configuration properties for the neverpile ignite support.
 * 
 * Some of these properties are not used directly but just to document properties defined using
 * {@code @Value} and {@code @ConditionalOnProperty} constructs.
 */
@Component
@ConfigurationProperties(prefix = "neverpile-eureka.ignite")
public class IgniteConfigurationProperties {
  public static class Persistence {
    /**
     * Whether to enable persistence.
     */
    private boolean enabled;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(final boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static class WAL {
    /**
     * Whether to enable the Ignite-based write-ahead-log.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(final boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static class Cache {
    /**
     * Whether to enable the Ignite-based cache.
     */
    private boolean enabled = true;

    private Map<String, CacheConfiguration<Object, Object>> configurations = new HashMap<>();
    
    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(final boolean enabled) {
      this.enabled = enabled;
    }

    public Map<String, CacheConfiguration<Object, Object>> getConfigurations() {
      return configurations;
    }

    public void setConfigurations(final Map<String, CacheConfiguration<Object, Object>> cacheConfigs) {
      this.configurations = cacheConfigs;
    }
  }
  
  // This class hierarchy is mainly for configuration documentation purposes. The actual
  // Values are directly injected into the respective finder classes.
  public static class Finders {
    public static class Finder {
      private boolean enabled;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
      }
    }

    public static class Multicast extends Finder {
      /**
       * Sets local host address used by the IP finder. If provided address is non-loopback then
       * multicast socket is bound to this interface. If local address is not set or is any local
       * address then IP finder creates multicast sockets for all found non-loopback addresses.
       */
      private String localAddress;

      /**
       * Sets number of attempts to send multicast address request. IP finder re-sends request only
       * in case if no reply for previous request is received.
       * <p>
       * If not provided, default value is 2.
       */
      private int addressRequestAttempts;

      /**
       * Sets IP address of multicast group.
       * <p>
       * If not provided, default value is 228.1.2.4.
       */
      private String multicastGroup;

      /**
       * Sets port number which multicast messages are sent to.
       * <p>
       * If not provided, default value is 47400.
       */
      private int multicastPort;

      /**
       * Sets time in milliseconds IP finder waits for reply to multicast address request.
       * <p>
       * If not provided, default value is 500ms.
       */
      private int responseWaitTime;

      /**
       * Set the default time-to-live for multicast packets sent out on this IP finder in order to
       * control the scope of the multicast.
       * <p>
       * The TTL has to be in the range 0 &lt;= TTL &lt;= 255}.
       * <p>
       * If TTL is 0, packets are not transmitted on the network, but may be delivered locally.
       * <p>
       * Default value is -1 which corresponds to system default value.
       */
      private int timeToLive;

      public String getLocalAddress() {
        return localAddress;
      }

      public void setLocalAddress(final String localAddress) {
        this.localAddress = localAddress;
      }

      public int getAddressRequestAttempts() {
        return addressRequestAttempts;
      }

      public void setAddressRequestAttempts(final int addressRequestAttempts) {
        this.addressRequestAttempts = addressRequestAttempts;
      }

      public String getMulticastGroup() {
        return multicastGroup;
      }

      public void setMulticastGroup(final String multicastGroup) {
        this.multicastGroup = multicastGroup;
      }

      public int getMulticastPort() {
        return multicastPort;
      }

      public void setMulticastPort(final int multicastPort) {
        this.multicastPort = multicastPort;
      }

      public int getResponseWaitTime() {
        return responseWaitTime;
      }

      public void setResponseWaitTime(final int responseWaitTime) {
        this.responseWaitTime = responseWaitTime;
      }

      public int getTimeToLive() {
        return timeToLive;
      }

      public void setTimeToLive(final int timeToLive) {
        this.timeToLive = timeToLive;
      }
    }

    public static class StaticIp extends Finder {
      /**
       * The list of addresses used entrypoints for discovery.
       * <p>
       * Addresses may be represented as follows:
       * <ul>
       * <li>IP address (e.g. 127.0.0.1, 9.9.9.9, etc);</li>
       * <li>IP address and port (e.g. 127.0.0.1:47500, 9.9.9.9:47501, etc);</li>
       * <li>IP address and port range (e.g. 127.0.0.1:47500..47510, 9.9.9.9:47501..47504,
       * etc);</li>
       * <li>Hostname (e.g. host1.com, host2, etc);</li>
       * <li>Hostname and port (e.g. host1.com:47500, host2:47502, etc).</li>
       * <li>Hostname and port range (e.g. host1.com:47500..47510, host2:47502..47508, etc).</li>
       * </ul>
       * <p>
       * If port is 0 or not provided then default port will be used (depends on discovery SPI
       * configuration).
       * <p>
       * If port range is provided (e.g. host:port1..port2) the following should be considered:
       * <ul>
       * <li>port1 &lt; port2 should be true</li>
       * <li>Both port1 and port2 should be greater than 0.</li>
       * </ul>
       */
      private List<String> addresses = new ArrayList<>();

      public List<String> getAddresses() {
        return addresses;
      }

      public void setAddresses(final List<String> addresses) {
        this.addresses = addresses;
      }
    }

    public static class Filesystem extends Finder {
      /**
       * The path to the shared file system directory. If the path is not provided, then a default
       * path will be used and only local nodes will discover each other. To enable discovery over
       * network you must provide a path to a shared directory explicitly.
       * <p>
       * The directory will contain empty files named like the following 192.168.1.136#1001.
       */
      private String path;

      public String getPath() {
        return path;
      }

      public void setPath(final String path) {
        this.path = path;
      }
    }

    public static class Cloud extends Finder {
      private String credential;
      private String credentialPath;
      private String provider;
      private List<String> regions = new ArrayList<>();
      private List<String> zones = new ArrayList<>();

      public String getCredential() {
        return credential;
      }
      public void setCredential(final String credential) {
        this.credential = credential;
      }
      public String getCredentialPath() {
        return credentialPath;
      }
      public void setCredentialPath(final String credentialPath) {
        this.credentialPath = credentialPath;
      }
      public String getProvider() {
        return provider;
      }
      public void setProvider(final String provider) {
        this.provider = provider;
      }
      public List<String> getRegions() {
        return regions;
      }
      public void setRegions(final List<String> regions) {
        this.regions = regions;
      }
      public List<String> getZones() {
        return zones;
      }
      public void setZones(final List<String> zones) {
        this.zones = zones;
      }
    }

    public static class S3 extends Finder {
      public static class MutableAWSCredentials {
        private String accessKey;
        private String secretKey;

        /*
         * (non-Javadoc)
         * 
         * @see com.amazonaws.auth.AWSCredentials#getAWSAccessKeyId()
         */
        public String getAWSAccessKeyId() {
          return accessKey;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.amazonaws.auth.AWSCredentials#getAWSSecretKey()
         */
        public String getAWSSecretKey() {
          return secretKey;
        }

        public String getAccessKey() {
          return accessKey;
        }

        public void setAccessKey(final String accessKey) {
          this.accessKey = accessKey;
        }

        public String getSecretKey() {
          return secretKey;
        }

        public void setSecretKey(final String secretKey) {
          this.secretKey = secretKey;
        }
      }

      private MutableAWSCredentials awsCredentials;

      private String bucketEndpoint;
      private String bucketName;
      private ClientConfiguration clientConfiguration = new ClientConfiguration();
      private String keyPrefix = "ignite-finder/";

      public MutableAWSCredentials getAwsCredentials() {
        return awsCredentials;
      }

      public void setAwsCredentials(final MutableAWSCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
      }

      public String getBucketEndpoint() {
        return bucketEndpoint;
      }

      public void setBucketEndpoint(final String bucketEndpoint) {
        this.bucketEndpoint = bucketEndpoint;
      }

      public String getBucketName() {
        return bucketName;
      }

      public void setBucketName(final String bucketName) {
        this.bucketName = bucketName;
      }

      public ClientConfiguration getClientConfiguration() {
        return clientConfiguration;
      }

      public void setClientConfiguration(final ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
      }

      public String getKeyPrefix() {
        return keyPrefix;
      }

      public void setKeyPrefix(final String keyPrefix) {
        this.keyPrefix = keyPrefix;
      }
    }

    private Multicast multicast = new Multicast();

    private StaticIp staticIp = new StaticIp();

    private Filesystem filesystem = new Filesystem();

    private Cloud cloud = new Cloud();

    private S3 s3 = new S3();

    public Multicast getMulticast() {
      return multicast;
    }

    public void setMulticast(final Multicast multicast) {
      this.multicast = multicast;
    }

    public StaticIp getStaticIp() {
      return staticIp;
    }

    public void setStaticIp(final StaticIp staticIp) {
      this.staticIp = staticIp;
    }

    public Filesystem getFilesystem() {
      return filesystem;
    }

    public void setFilesystem(final Filesystem filesystem) {
      this.filesystem = filesystem;
    }

    public Cloud getCloud() {
      return cloud;
    }

    public void setCloud(final Cloud cloud) {
      this.cloud = cloud;
    }

    public S3 getS3() {
      return s3;
    }

    public void setS3(final S3 s3) {
      this.s3 = s3;
    }
  }

  /**
   * Whether to enable the ignite cluster subsystem.
   */
  private boolean enabled;

  /**
   * Persistence-related settings.
   */
  private Persistence persistence = new Persistence();

  /**
   * Write-ahead-log related settings.
   */
  private WAL wal = new WAL();

  /**
   * Cache-related settings.
   */
  private Cache cache = new Cache();

  /**
   * Discovery-related settings.
   */
  private Finders finder = new Finders();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public Persistence getPersistence() {
    return persistence;
  }

  public void setPersistence(final Persistence persistence) {
    this.persistence = persistence;
  }

  public WAL getWal() {
    return wal;
  }

  public void setWal(final WAL wal) {
    this.wal = wal;
  }

  public Finders getFinder() {
    return finder;
  }

  public void setFinder(final Finders finder) {
    this.finder = finder;
  }

  public Cache getCache() {
    return cache;
  }

  public void setCache(final Cache cache) {
    this.cache = cache;
  }
}
