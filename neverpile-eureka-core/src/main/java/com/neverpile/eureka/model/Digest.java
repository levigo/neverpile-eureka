package com.neverpile.eureka.model;

import java.util.Arrays;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A cryptographic digest used to verify the integrity of an object")
public class Digest {
  private HashAlgorithm algorithm;
  
  private byte[] bytes;

  @Schema(description = "The algorithm used to compute the digest")
  public HashAlgorithm getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(final HashAlgorithm algorithm) {
    this.algorithm = algorithm;
  }

  @Schema(description = "The hash value as a byte string")
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