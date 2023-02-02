package com.neverpile.eureka.objectstore.s3;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.adobe.testing.s3mock.junit4.S3MockRule;
import com.amazonaws.services.s3.AmazonS3;
import com.neverpile.eureka.api.objectstore.AbstractObjectStoreServiceTest;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ContextConfiguration(classes = TestConfiguration.class)
@EnableAutoConfiguration()
public class S3ObjectStoreServiceTest extends AbstractObjectStoreServiceTest {

  private static final String BUCKET_NAME = "unit-tests";

  @ClassRule
  public static final S3MockRule s3 = S3MockRule.builder().withHttpPort(9876).build();

  @Before
  public void createBucket() {
    AmazonS3 s3Client = s3.createS3Client();
    try {
      s3Client.listObjects(BUCKET_NAME).getObjectSummaries().forEach(
          (object -> s3Client.deleteObject(BUCKET_NAME, object.getKey())));
      s3Client.deleteBucket(BUCKET_NAME);
    } catch (Exception e) {
      // ignored
    }
    s3Client.createBucket(BUCKET_NAME);
  }

}
