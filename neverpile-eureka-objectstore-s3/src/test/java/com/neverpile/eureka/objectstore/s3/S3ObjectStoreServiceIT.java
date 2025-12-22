package com.neverpile.eureka.objectstore.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.neverpile.eureka.api.objectstore.AbstractObjectStoreServiceTest;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

@SpringBootTest()
@ContextConfiguration(classes = TestConfiguration.class)
@EnableAutoConfiguration()
public class S3ObjectStoreServiceIT extends AbstractObjectStoreServiceTest {

  private static final String BUCKET_NAME = "unit-tests";

  @Autowired
  S3ConnectionConfiguration connectionConfiguration;

  @BeforeEach
  public void createBucket() {
    S3Client s3Client = connectionConfiguration.createClient();
    try {
      // List and delete all objects in the bucket
      ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(BUCKET_NAME).build();

      ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

      listResponse.contents().forEach(object -> {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(BUCKET_NAME).key(object.key()).build();
        s3Client.deleteObject(deleteRequest);
      });

      // Delete the bucket
      DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(BUCKET_NAME).build();
      s3Client.deleteBucket(deleteBucketRequest);
    } catch (Exception e) {
      // ignored
    }

    // Create the bucket
    CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket(BUCKET_NAME).build();
    s3Client.createBucket(createBucketRequest);
  }

  // For the problem with object collision using MinIO see:
  // https://min.io/docs/minio/linux/operations/concepts/thresholds.html#conflicting-objects

  // This is ignored because of a deviation in behaviour of MinIO that prevents an Object to be discovered
  // with a prefix of an already existing object.
  @Disabled
  @Override
  @Test
  public void testThat_objectWontGetDeletedIfSuffixGetsDeleted() {
  }

  // This is ignored because of a deviation in behaviour of MinIO that prevents an Object to be discovered
  // with a prefix of an already existing object.
  @Disabled
  @Override
  @Test
  public void testThat_ObjectNamesCanBeListedViaPrefix() {
  }

  // This is ignored because of a deviation in behaviour of MinIO that prevents an Object to be discovered
  // with a prefix of an already existing object.
  @Disabled
  @Override
  @Test
  public void testThat_AllObjectsCanBeListed() {
  }
}
