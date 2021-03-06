package com.neverpile.eureka.objectstore.cassandra;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CassandraObjectDataRepository extends CassandraRepository<CassandraObjectData, Void> {

  @Query("select * from objectdata where objectName = ?0 order by version DESC limit ?1 ;")
  Iterable<CassandraObjectData> findByObjectName(String documentId, int batchSize);

  @Query("select * from objectdata where objectName = ?0 and version = ?1 order by version, chunkNo ASC limit ?2 ;")
  Iterable<CassandraObjectData> findByObjectNameAndVersion(String documentId, int version, int batchSize);

  @Query("select * from objectdata where objectName = ?0 and version = ?1 and chunkNo >= ?2 order by version, chunkNo ASC limit ?3 ;")
  Iterable<CassandraObjectData> findByObjectNameAndChunkNoGreaterThanEqual(String documentId, int version, int startChunk, int batchSize);

  @Query("delete from objectdata where objectName = ?0 ;")
  void deleteByObjectName(String objectName);

  @Query("delete from objectdata where objectName = ?0 and version = ?1 ;")
  void deleteByObjectNameAndByVersion(String objectName, int version);

  @Query("delete from objectdata where objectName = ?0 and version > ?1 ;")
  void deleteByObjectNameAndVersionGreaterThan(String objectName, int version);
}
