package com.neverpile.eureka.objectstore.cassandra;

import java.util.stream.Stream;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import com.neverpile.eureka.model.ObjectName;

@Repository
public interface CassandraPrefixRepository extends CassandraRepository<CassandraObjectPrefix, String> {

  @Query("select suffix, prefix from prefix where prefix = ?0 order by suffix ASC ;")
  Stream<CassandraObjectPrefix> findPrefixes(String prefix);

  @Query("select count(*) from prefix where prefix = ?0 ;")
  int countSuffixes(String prefix);
  
  default Stream<CassandraObjectPrefix> findPrefixes(final ObjectName prefix) {
    return findPrefixes(String.join(CassandraObjectPrefix.PREFIX_DELIMITER, prefix.to()));
  };
}
