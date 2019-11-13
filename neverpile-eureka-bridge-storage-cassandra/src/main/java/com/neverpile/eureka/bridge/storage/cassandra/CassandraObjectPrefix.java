package com.neverpile.eureka.bridge.storage.cassandra;

import java.util.Arrays;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import com.neverpile.eureka.model.ObjectName;

@Table(value = "prefix")
public class CassandraObjectPrefix {
  public static final String PREFIX_DELIMITER = "\t";

  @PrimaryKeyColumn(name = "prefix", ordinal = 0, type = PrimaryKeyType.PARTITIONED, ordering = Ordering.ASCENDING)
  private String prefix;

  @PrimaryKeyColumn(name = "suffix", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.ASCENDING)
  private String suffix;

  public CassandraObjectPrefix(final String prefix, final String suffix) {
    super();
    this.prefix = prefix;
    this.suffix = suffix;
  }

  public CassandraObjectPrefix() {
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(final String prefix) {
    this.prefix = prefix;
  }

  public String getSuffix() {
    return suffix;
  }

  public void setSuffix(final String suffix) {
    this.suffix = suffix;
  }

  public static CassandraObjectPrefix from(final ObjectName objectName) {
    String[] components = objectName.to();
    return new CassandraObjectPrefix(String.join(PREFIX_DELIMITER, Arrays.copyOf(components, components.length - 1)),
        components[components.length - 1]);
  }

  public ObjectName toObjectName() {
    return ObjectName.of(this.getPrefix().split(PREFIX_DELIMITER)).append(this.getSuffix());
  }
}
