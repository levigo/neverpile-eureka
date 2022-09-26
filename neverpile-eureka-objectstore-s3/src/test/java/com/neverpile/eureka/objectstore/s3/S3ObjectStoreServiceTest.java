//package com.neverpile.eureka.objectstore.s3;
//
//import org.junit.Before;
//import org.junit.ClassRule;
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
//import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//import com.adobe.testing.s3mock.junit4.S3MockRule;
//import com.neverpile.eureka.api.objectstore.AbstractObjectStoreServiceTest;
//
//@SpringBootTest(properties = {
//    "spring.jta.atomikos.properties.max-timeout=600000", "spring.jta.atomikos.properties.default-jta-timeout=600000",
//    "spring.transaction.default-timeout=900"
//})
//@ContextConfiguration(classes = TestConfiguration.class)
//@EnableTransactionManagement
//public class S3ObjectStoreServiceTest extends AbstractObjectStoreServiceTest {
//
//  private static final String BUCKET_NAME = "unit-tests";
//
//  @ClassRule
//  public static final S3MockRule s3 = S3MockRule.builder().withHttpPort(9876).build();
//
//  @Before
//  public void createBucket() {
//    try {
//      s3.createS3Client().deleteBucket(BUCKET_NAME);
//    } catch (Exception e) {
//      // ignored
//    }
//    s3.createS3Client().createBucket(BUCKET_NAME);
//  }
//
//}
