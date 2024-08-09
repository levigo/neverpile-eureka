package com.neverpile.eureka.objectstore.ehcache;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;

import com.neverpile.eureka.api.objectstore.AbstractObjectStoreServiceTest;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "neverpile-eureka.bridge.storage.ehcache.rootPath=target/test-store")
@ContextConfiguration(classes = TestConfig.class)
@EnableAutoConfiguration()
public class EhcacheObjectStoreServiceTest extends AbstractObjectStoreServiceTest {

  @BeforeEach
  public void setNewCache() {
    objectStore = new EhcacheObjectStoreService();
  }
}
