package com.neverpile.eureka.objectstore.s3;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.endpoints.internal.DefaultStsEndpointProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

/**
 * Configuration properties for an S3 connection.
 */
@Component
@ConfigurationProperties(prefix = "neverpile-eureka.storage.s3.connection", ignoreUnknownFields = true)
public class S3ConnectionConfiguration implements Serializable {
  @Serial
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
  private String stsEndpoint;
  private String signingRegion = Region.EUSC_DE_EAST_1.id();
  private SignatureType signatureType;
  private String accessKeyId;
  private String secretAccessKey;
  private String defaultBucketName;
  private AccessStyle accessStyle = AccessStyle.Automatic;
  private ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder().build();

  private boolean disableCertificateChecking;
  private String roleArn;
  private String roleSessionName;
  private int durationSeconds;

  public S3Client createClient() {
    AwsCredentialsProvider credentialsProvider;

    AwsBasicCredentials basicAWSCredentials = AwsBasicCredentials.create(getAccessKeyId(), getSecretAccessKey());
    if (roleArn != null && !roleArn.isEmpty()) {
      StsClient stsClient = StsClient.builder().credentialsProvider(
          StaticCredentialsProvider.create(basicAWSCredentials)).endpointProvider(
          new DefaultStsEndpointProvider()).region(Region.of(this.getSigningRegion())).endpointOverride(
          URI.create(this.getStsEndpoint())).build();

      credentialsProvider = StsAssumeRoleCredentialsProvider.builder().refreshRequest(
          AssumeRoleRequest.builder()
            .roleArn(roleArn)
            .roleSessionName(getRoleSessionName())
            .durationSeconds(getDurationSeconds())
            .build()).stsClient(
          stsClient).build();
    } else {
      // Use basic credentials if roleArn is not set
      credentialsProvider = StaticCredentialsProvider.create(basicAWSCredentials);
    }

    System.setProperty("aws.disableCertChecking", Boolean.toString(disableCertificateChecking));

    return S3Client.builder().credentialsProvider(credentialsProvider).endpointOverride(
        URI.create(this.getEndpoint())).region(Region.of(this.getSigningRegion())).overrideConfiguration(
        getClientConfiguration()).forcePathStyle(accessStyle == AccessStyle.Path).build();
  }

  public String getRoleArn() {
    return roleArn;
  }

  public void setRoleArn(String roleArn) {
    this.roleArn = roleArn;
  }

  public String getRoleSessionName() {
    return roleSessionName;
  }

  public void setRoleSessionName(String roleSessionName) {
    this.roleSessionName = roleSessionName;
  }

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds(int durationSeconds) {
    this.durationSeconds = durationSeconds;
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

  public String getStsEndpoint() {
    return stsEndpoint;
  }

  public void setStsEndpoint(final String stsEndpointUrl) {
    this.stsEndpoint = stsEndpointUrl;
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

  public ClientOverrideConfiguration getClientConfiguration() {
    return clientConfiguration;
  }

  public void setClientConfiguration(ClientOverrideConfiguration clientConfiguration) {
    this.clientConfiguration = clientConfiguration;
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