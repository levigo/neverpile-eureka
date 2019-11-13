package com.neverpile.eureka.bridge.storage.fs;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.annotation.RequestScope;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.impl.tx.lock.NoOpDistributedLock;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.tx.lock.DistributedLock;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.local.DefaultTransactionWAL;
import com.neverpile.eureka.tx.wal.local.FileBasedWAL;


@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionalFileHandlingTest {

  @Configuration
  @EnableTransactionManagement
  @EnableAutoConfiguration
  public static class ServiceConfig {
    @Bean
    WriteAheadLog fileBasedWal() {
      return new FileBasedWAL();
    }

    @Bean
    @RequestScope
    TransactionWAL wal() {
      return new DefaultTransactionWAL();
    }

    @Bean
    FilesystemObjectStoreService filesystemObjectStoreService() {
      return new FilesystemObjectStoreService();
    }
    
    @Bean
    DistributedLock lock() {
      return new NoOpDistributedLock();
    }
  }

  @Autowired
  TransactionWAL wal;

  @Autowired
  TransactionTemplate transactionTemplate;

  @Autowired
  FilesystemObjectStoreService oss;

  @Before
  @After
  public void cleanup() {
    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        oss.delete(ObjectName.of());
        return null;
      }
    });
  }

  @Test
  public void testThat_objectsNamesCanBePrefixesOfOtherObjectNames() {
    ObjectName someObject = ObjectName.of("foo", "bar", "baz");
    ObjectName prefixName = ObjectName.of("foo", "bar");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        oss.put(someObject, ObjectStoreService.NEW_VERSION, createDummyStream());
        oss.put(prefixName, ObjectStoreService.NEW_VERSION, toStream("bar"));
        return null;
      }
    });

    assertObjectContent(someObject, "foo");
    assertObjectContent(prefixName, "bar");
  }

  @Test
  public void testThat_objectsArePreservedOnCommit() {
    ObjectName someObject = ObjectName.of("foo", "bar", "baz");

    assertThat(oss.get(someObject), nullValue());

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        oss.put(someObject, ObjectStoreService.NEW_VERSION, createDummyStream());
        return null;
      }
    });

    assertThat(oss.get(someObject), notNullValue());
  }

  @Test
  public void testThat_rollbackOfDeletionWorks() {
    ObjectName someObject = ObjectName.of("foo", "bar", "baz");

    assertThat(oss.get(someObject), nullValue());

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        oss.put(someObject, ObjectStoreService.NEW_VERSION, createDummyStream());
        return null;
      }
    });

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        oss.delete(someObject);
        status.setRollbackOnly();
        return null;
      }
    });

    assertThat(oss.get(someObject), notNullValue());
  }

  @Test
  public void testThat_recusriveDeletionWorks() {
    ObjectName o1 = ObjectName.of("foo", "bar", "baz1");
    ObjectName o2 = ObjectName.of("foo", "bar", "baz2", "baz3");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        oss.put(o1, ObjectStoreService.NEW_VERSION, createDummyStream());
        oss.put(o2, ObjectStoreService.NEW_VERSION, createDummyStream());
        return null;
      }
    });

    assertThat(oss.get(o1), notNullValue());
    assertThat(oss.get(o2), notNullValue());

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        oss.delete(ObjectName.of("foo", "bar"));
        return null;
      }
    });

    assertThat(oss.get(o1), nullValue());
    assertThat(oss.get(o2), nullValue());
  }

  @Test
  public void testThat_rollbackOfDirectoryDeletionWorks() {
    ObjectName o1 = ObjectName.of("foo", "bar", "baz1");
    ObjectName o2 = ObjectName.of("foo", "bar", "baz2", "baz3");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        oss.put(o1, ObjectStoreService.NEW_VERSION, createDummyStream());
        oss.put(o2, ObjectStoreService.NEW_VERSION, createDummyStream());
        return null;
      }
    });

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        oss.delete(ObjectName.of("foo", "bar"));
        status.setRollbackOnly();
        return null;
      }
    });

    assertThat(oss.get(o1), notNullValue());
    assertThat(oss.get(o2), notNullValue());
  }

  @Test
  public void testThat_rollbackOfOverwriteWorks() throws IOException {
    ObjectName someObject = ObjectName.of("foo", "bar", "baz");

    assertThat(oss.get(someObject), nullValue());

    // write "foo" into object
    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        oss.put(someObject, ObjectStoreService.NEW_VERSION, createDummyStream());
        return null;
      }
    });

    // replace with "bar"
    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        String contents = "bar";
        oss.put(someObject, String.format("%06X", 1), toStream(contents));
        assertObjectContent(someObject, contents);
        status.setRollbackOnly();
        return null;
      }
    });

    InputStream is = oss.get(someObject).getInputStream();
    assertThat(StreamUtils.copyToString(is, Charset.defaultCharset()), startsWith("foo"));
    is.close();
  }

  private ByteArrayInputStream toStream(final String contents) {
    return new ByteArrayInputStream(contents.getBytes());
  }

  private void assertObjectContent(final ObjectName name, final String expectedContent) {
    try {
      InputStream is = oss.get(name).getInputStream();
      assertThat(StreamUtils.copyToString(is, Charset.defaultCharset()), startsWith(expectedContent));
      is.close();
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }

  private ByteArrayInputStream createDummyStream() {
    return new ByteArrayInputStream("foo".getBytes());
  }
}
