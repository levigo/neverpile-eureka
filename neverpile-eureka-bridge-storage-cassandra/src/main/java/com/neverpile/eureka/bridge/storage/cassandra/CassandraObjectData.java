package com.neverpile.eureka.bridge.storage.cassandra;

import java.nio.ByteBuffer;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Table(value = "objectdata")
public class CassandraObjectData {

  @PrimaryKeyColumn(name = "objectName", ordinal = 0, type = PrimaryKeyType.PARTITIONED, ordering = Ordering.DESCENDING)
  private String objectName;

  @PrimaryKeyColumn(name = "version", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
  private int version;

  @PrimaryKeyColumn(name = "chunkNo", ordinal = 2, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
  private int chunkNo;

  @Column
  private ByteBuffer data;

  public CassandraObjectData(final String objectName, final int version, final int chunkNo, final ByteBuffer data) {
    this.objectName = objectName;
    this.chunkNo = chunkNo;
    this.data = data;
    this.version = version;
  }

  public CassandraObjectData(final String objectName, final int version) {
    this.objectName = objectName;
  }

  public CassandraObjectData(final String objectName) {
    this.objectName = objectName;
  }

  public CassandraObjectData() { }

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

  public int getChunkNo() {
    return chunkNo;
  }

  public void setChunkNo(final int chunkNo) {
    this.chunkNo = chunkNo;
  }

  public ByteBuffer getData() {
    return data;
  }

  public void setData(final ByteBuffer data) {
    this.data = data;
  }
}
