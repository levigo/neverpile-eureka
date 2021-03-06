package com.neverpile.eureka.objectstore.fs;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;

import com.neverpile.eureka.api.objectstore.AbstractObjectStoreServiceTest;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "neverpile-eureka.bridge.storage.filesystem.rootPath=target/test-store")
@ContextConfiguration(classes = TestConfig.class)
@EnableAutoConfiguration(exclude = {
    CassandraDataAutoConfiguration.class
})
public class FilesystemObjectStoreServiceTest extends AbstractObjectStoreServiceTest {
  @Before
  public void cleanStore() throws IOException {
    FileSystemUtils.deleteRecursively(Paths.get("target/test-store"));
  }
}
