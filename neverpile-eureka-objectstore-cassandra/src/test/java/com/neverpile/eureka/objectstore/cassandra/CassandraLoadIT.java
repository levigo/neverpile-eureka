package com.neverpile.eureka.objectstore.cassandra;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import jakarta.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CassandraTestConfig.class)
@EnableAutoConfiguration(exclude={CassandraDataAutoConfiguration.class})
public class CassandraLoadIT {

  @Autowired
  private CassandraObjectStoreService objectStore;

  @PostConstruct
  public void cleanUp() {
    objectStore.truncateDataTable();
    objectStore.truncateObjectTable();
  }

  private static final int times = 2 * 1024 * 1024; // * 10 bytes

  private static final String Content = "1234567890";


  @Test
  @Transactional
  public void test_1mb_1_1() {
    test(1024 * 1024, 1, 1);
  }

  @Test
  @Transactional
  public void test_1mb_1_100() {
    test(1024 * 1024, 1, 100);
  }

  @Test
  @Transactional
  public void test_1mb_1_200() {
    test(1024 * 1024, 1, 200);
  }


  @Test
  @Transactional
  public void test_1mb_5_1() {
    test(1024 * 1024, 5, 1);
  }

  @Test
  @Transactional
  public void test_1mb_5_100() {
    test(1024 * 1024, 5, 100);
  }

  @Test
  @Transactional
  public void test_1mb_5_200() {
    test(1024 * 1024, 5, 200);
  }

  @Test
  @Transactional
  public void test_1mb_10_1() {
    test(1024 * 1024, 10, 1);
  }

  @Test
  @Transactional
  public void test_1mb_10_100() {
    test(1024 * 1024, 10, 100);
  }

  @Test
  @Transactional
  public void test_1mb_10_200() {
    test(1024 * 1024, 10, 200);
  }

  @Test
  @Transactional
  public void test_1mb_15_1() {
    test(1024 * 1024, 15, 1);
  }

  @Test @Transactional public void test_1mb_15_100() {
    test(1024 * 1024, 15, 100);
  }

  @Test
  @Transactional
  public void test_1mb_15_200() {
    test(1024 * 1024, 15, 200);
  }

  private void test(int maxBufferSize, int maxRequestQueryBatchSize, int maxResponseQueryBatchSize) {

    objectStore.setMaxBufferSize(maxBufferSize);
    objectStore.setMaxRequestQueryBatchSize(maxRequestQueryBatchSize);
    objectStore.setMaxResponseQueryBatchSize(maxResponseQueryBatchSize);

    TestDataInputStream is = new TestDataInputStream(Content, times);
    long t = System.currentTimeMillis();
    System.out.println("PUT");
    objectStore.put(ObjectName.of("Test"), ObjectStoreService.NEW_VERSION, is);
    System.out.println("took " + (System.currentTimeMillis() - t) + "s");
    t = System.currentTimeMillis();
    System.out.println("GET");
    ObjectStoreService.StoreObject so = objectStore.get(ObjectName.of("Test"));
    System.out.println("took " + (System.currentTimeMillis() - t) + "ms");
    t = System.currentTimeMillis();
    System.out.println("Count");
    assertEquals(is.getTestDataLength(), countInputStream(so.getInputStream()));
    System.out.println("took " + (System.currentTimeMillis() - t) + "ms");
  }

  class TestDataInputStream extends InputStream {
    private byte[] sample;
    private long pos = 0;
    private final long total;

    TestDataInputStream(String content, int times) {
      super();
      this.sample = content.getBytes();
      this.total = (long) sample.length * times;
    }

    @Override
    public int read() {
      return pos < total ? sample[(int) (pos++ % sample.length)] : -1;
    }

    @Override
    public int available() {
      return (total - pos > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (total - pos));
    }

    long getTestDataLength() {
      return total;
    }
  }

  private int countInputStream(InputStream is) {
    int count = 0;
    try {
      while (is.read() != -1) {
        count++;
      }
    } catch (IOException e) {
      return -1;
    }
    return count;
  }
}
