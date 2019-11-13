package com.neverpile.eureka.bridge.storage.cassandra;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = "object")
public class CassandraObject {
  @PrimaryKeyColumn(name = "objectName", ordinal = 0, type = PrimaryKeyType.PARTITIONED, ordering = Ordering.DESCENDING)
  private String objectName;

  @PrimaryKeyColumn(name = "version", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
  private int version;

  @Column
  private int dataChunkCount;

  public CassandraObject(final String objectName, final int version, final int dataChunkCount) {
    this.objectName = objectName;
    this.version = version;
    this.dataChunkCount = dataChunkCount;
  }

  public CassandraObject(final String objectName, final int version) {
    this.objectName = objectName;
    this.version = version;
  }

  public CassandraObject(final String objectName) {
    this.objectName = objectName;
  }

  public CassandraObject() {}

  public String getObjectName() {
    return objectName;
  }

  public void setObjectName(final String objectName) {
    this.objectName = objectName;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(final int version) {
    this.version = version;
  }

  public int getDataChunkCount() {
    return dataChunkCount;
  }

  public void setDataChunkCount(final int dataChunkCount) {
    this.dataChunkCount = dataChunkCount;
  }
}
