package com.neverpile.eureka.objectstore.s3;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.amazonaws.services.s3.AmazonS3;
import com.neverpile.eureka.api.objectstore.AbstractObjectStoreServiceTest;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ContextConfiguration(classes = TestConfiguration.class)
@EnableAutoConfiguration()
public class S3ObjectStoreServiceIT extends AbstractObjectStoreServiceTest {

  private static final String BUCKET_NAME = "unit-tests";

  @Autowired
  S3ConnectionConfiguration connectionConfiguration;

  @Before
  public void createBucket() {
    AmazonS3 s3Client = connectionConfiguration.createClient();
    try {
      s3Client.listObjects(BUCKET_NAME).getObjectSummaries().forEach(
          (object -> s3Client.deleteObject(BUCKET_NAME, object.getKey())));
      s3Client.deleteBucket(BUCKET_NAME);
    } catch (Exception e) {
      // ignored
    }
    s3Client.createBucket(BUCKET_NAME);
  }

  // For the problem with object collision using MinIO see:
  // https://min.io/docs/minio/linux/operations/concepts/thresholds.html#conflicting-objects

  // This is ignored because of a deviation in behaviour of MinIO that prevents an Object to be discovered
  // with a prefix of an already existing object.
  @Ignore
  @Override
  public void testThat_objectWontGetDeletedIfSuffixGetsDeleted() {
  }

  // This is ignored because of a deviation in behaviour of MinIO that prevents an Object to be discovered
  // with a prefix of an already existing object.
  @Ignore
  @Override
  public void testThat_ObjectNamesCanBeListedViaPrefix() {
  }

  // This is ignored because of a deviation in behaviour of MinIO that prevents an Object to be discovered
  // with a prefix of an already existing object.
  @Ignore
  @Override
  public void testThat_AllObjectsCanBeListed() {
  }
}
