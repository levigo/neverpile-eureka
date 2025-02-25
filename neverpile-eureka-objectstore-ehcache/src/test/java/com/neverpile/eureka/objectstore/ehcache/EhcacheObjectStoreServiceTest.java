package com.neverpile.eureka.objectstore.ehcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

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
