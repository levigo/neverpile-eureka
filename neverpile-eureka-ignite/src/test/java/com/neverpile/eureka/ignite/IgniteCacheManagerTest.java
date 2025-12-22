package com.neverpile.eureka.ignite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.neverpile.eureka.ignite.cache.IgniteCacheManager;

@SpringBootTest
@ContextConfiguration(classes = TestConfig.class)
@EnableAutoConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IgniteCacheManagerTest {
  @Autowired
  IgniteCacheManager cacheManager;
  
  @Test
  public void testThat_cacheGetSignalsMisses() {
    assertThat(cacheManager.getCache("test").get("doesNotExist_cacheGetSignalsMisses")).isNull();
  }
  
  @Test
  public void testThat_valueLoaderWorks() {
    assertThat(cacheManager.getCache("test").get("doesNotExist_valueLoaderWorks", () -> "foo")).isEqualTo("foo");
  }
  
  @Test
  public void testThat_typeCasting() {
    Cache c = cacheManager.getCache("test");
    c.put("foo", "bar");
    
    assertThat(c.get("foo", String.class)).isEqualTo("bar");
  }
  
  @Test
  public void testThat_cacheReturnsHits() {
    Cache c = cacheManager.getCache("test");
    c.put("foo", "bar");
    
    assertThat(c.get("foo").get()).isEqualTo("bar");
  }
}
