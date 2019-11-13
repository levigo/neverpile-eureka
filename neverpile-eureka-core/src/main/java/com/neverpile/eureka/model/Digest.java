package com.neverpile.eureka.model;

import java.util.Arrays;
import java.util.Objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "A cryptographic digest used to verify the integrity of an object")
public class Digest {
  private HashAlgorithm algorithm;
  
  private byte[] bytes;

  @ApiModelProperty("The algorithm used to compute the digest")
  public HashAlgorithm getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(final HashAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  @ApiModelProperty("The hash value as a byte string")
  public byte[] getBytes() {
    return bytes;
  }

  public void setBytes(final byte[] digestBytes) {
    this.bytes = digestBytes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(algorithm, bytes);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Digest other = (Digest) obj;
    if (algorithm != other.algorithm)
      return false;
    if (!Arrays.equals(bytes, other.bytes))
      return false;
    return true;
  }
}