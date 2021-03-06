package com.neverpile.eureka.objectstore.cassandra;

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CassandraObjectRepository extends CassandraRepository<CassandraObject, Void> {

  @Query("select * from object where objectName = ?0 order by version DESC limit 1;")
  Optional<CassandraObject> findByObjectName(String objectName);

  @Query("delete from object where objectName = ?0 ;")
  void deleteByObjectName(String objectName);

  @Query("delete from object where objectName = ?0 and version = ?1 ;")
  void deleteByObjectNameAndByVersion(String objectName, int version);

  @Query("delete from object where objectName = ?0 and version > ?1 ;")
  void deleteByObjectNameAndVersionGreaterThan(String objectName, int version);

  // https://github.com/spring-projects/spring-data-examples/tree/master/cassandra/java8
  @Query("select * from object")
  Stream<CassandraObject> findAllCassandraObjects();
}
