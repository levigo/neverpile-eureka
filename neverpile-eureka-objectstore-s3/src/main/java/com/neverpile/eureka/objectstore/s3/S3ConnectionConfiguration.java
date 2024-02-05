package com.neverpile.eureka.objectstore.s3;

import java.io.Serializable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Region;

/**
 * Configuration properties for an S3 connection.
 */
@Component
@ConfigurationProperties(prefix = "neverpile-eureka.storage.s3.connection", ignoreUnknownFields = true)
public class S3ConnectionConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public enum SignatureType {
    V2, V4;
  }

  public enum AccessStyle {
    /**
     * Amazon S3 supports virtual-hosted-style and path-style access in all Regions. The path-style
     * syntax, however, requires that you use the region-specific endpoint when attempting to access
     * a bucket.
     */
    Path,

    /**
     * The default behaviour is to detect which access style to use based on the configured
     * endpoint (an IP will result in path-style access) and the bucket being accessed (some buckets
     * are not valid DNS names).
     */
    Automatic
  }

  private String accountName;

  private String endpoint;

  private String signingRegion = Region.EU_Frankfurt.getFirstRegionId();

  private SignatureType signatureType;

  private String accessKeyId;

  private String secretAccessKey;

  private String defaultBucketName;

  private AccessStyle accessStyle = AccessStyle.Automatic;

  private ClientConfiguration clientConfiguration = new ClientConfiguration();

  private boolean disableCertificateChecking;

  public AmazonS3 createClient() {
    AWSCredentials credentials = new BasicAWSCredentials(getAccessKeyId(), getSecretAccessKey());

    System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY,
        Boolean.toString(disableCertificateChecking));

    return AmazonS3ClientBuilder.standard() //
        .withCredentials(new AWSStaticCredentialsProvider(credentials)) //
        .withEndpointConfiguration(
            new EndpointConfiguration(this.getEndpoint(), this.getSigningRegion())) //
        .withClientConfiguration(getClientConfiguration()) //
        .withPathStyleAccessEnabled(accessStyle == AccessStyle.Path) //
        .build();
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(final String accountName) {
    this.accountName = accountName;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(final String endpointUrl) {
    this.endpoint = endpointUrl;
  }

  public String getSigningRegion() {
    return signingRegion;
  }

  public void setSigningRegion(String signingRegion) {
    this.signingRegion = signingRegion;
  }

  public SignatureType getSignatureType() {
    return signatureType;
  }

  public void setSignatureType(final SignatureType signatureType) {
    this.signatureType = signatureType;
  }

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public void setAccessKeyId(final String accessKeyID) {
    this.accessKeyId = accessKeyID;
  }

  public String getSecretAccessKey() {
    return secretAccessKey;
  }

  public void setSecretAccessKey(final String secretAccessKey) {
    this.secretAccessKey = secretAccessKey;
  }

  public String getDefaultBucketName() {
    return defaultBucketName;
  }

  public void setDefaultBucketName(final String defaultBucketName) {
    this.defaultBucketName = defaultBucketName;
  }

  public ClientConfiguration getClientConfiguration() {
    return clientConfiguration;
  }

  public void setClientConfiguration(final ClientConfiguration client) {
    this.clientConfiguration = client;
  }

  public AccessStyle getAccessStyle() {
    return accessStyle;
  }

  public void setAccessStyle(final AccessStyle accessStyle) {
    this.accessStyle = accessStyle;
  }

  public boolean isDisableCertificateChecking() {
    return disableCertificateChecking;
  }

  public void setDisableCertificateChecking(final boolean disableCertificateChecking) {
    this.disableCertificateChecking = disableCertificateChecking;
  }
}
