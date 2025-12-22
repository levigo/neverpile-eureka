package com.neverpile.eureka.objectstore.fs;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.FileSystemUtils;

import com.neverpile.eureka.api.objectstore.AbstractObjectStoreServiceTest;

@SpringBootTest(properties = "neverpile-eureka.bridge.storage.filesystem.rootPath=target/test-store")
@ContextConfiguration(classes = TestConfig.class)
@EnableAutoConfiguration()
@EnableTransactionManagement
public class FilesystemObjectStoreServiceTest extends AbstractObjectStoreServiceTest {
  @BeforeEach
  public void cleanStore() throws IOException {
    FileSystemUtils.deleteRecursively(Path.of("target/test-store"));
  }
}
