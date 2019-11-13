package com.neverpile.eureka.bridge.storage.s3;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.adobe.testing.s3mock.junit4.S3MockRule;
import com.neverpile.eureka.api.objectstore.AbstractObjectStoreServiceTest;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
    "spring.jta.atomikos.properties.max-timeout=600000", "spring.jta.atomikos.properties.default-jta-timeout=600000",
    "spring.transaction.default-timeout=900"
})
@ContextConfiguration(classes = TestConfiguration.class)
@EnableTransactionManagement
public class S3ObjectStoreServiceTest extends AbstractObjectStoreServiceTest {
  static {
    // Disable spring security for the embedded S3-Mock.
    // See https://github.com/adobe/S3Mock/issues/130 for details
    System.setProperty("spring.autoconfigure.exclude",
        Stream.of(SecurityAutoConfiguration.class/*, ManagementWebSecurityAutoConfiguration.class*/)//
            .map(Class::getName).collect(Collectors.joining(",")));
  }

  @ClassRule
  public static final S3MockRule s3 = S3MockRule.builder().withHttpPort(9876).build();

  @Before
  public void createBucket() {
    try {
      s3.createS3Client().deleteBucket("unit-tests");
    } catch (Exception e) {
      // ignored
    }
    s3.createS3Client().createBucket("unit-tests");
  }

}
