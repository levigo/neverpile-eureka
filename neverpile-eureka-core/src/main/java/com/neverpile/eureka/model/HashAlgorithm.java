package com.neverpile.eureka.model;

import com.fasterxml.jackson.annotation.JsonProperty;


public enum HashAlgorithm {
  @JsonProperty("SHA-1")
  SHA_1("SHA-1"), @JsonProperty("SHA-256")
  SHA_256("SHA-256"), @JsonProperty("SHA-384")
  SHA_384("SHA-384"), @JsonProperty("SHA-512")
  SHA_512("SHA-512"), @JsonProperty("MD5")
  MD5("MD5");


  private final String value;

  HashAlgorithm(final String v) {
    value = v;
  }

  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static HashAlgorithm fromValue(final String v) {
    for (HashAlgorithm b : HashAlgorithm.values()) {
      if (String.valueOf(b.value).equals(v)) {
        return b;
      }
    }
    return null;
  }
}