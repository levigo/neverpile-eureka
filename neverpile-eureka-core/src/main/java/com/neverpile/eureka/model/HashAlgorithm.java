package com.neverpile.eureka.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import io.swagger.v3.oas.annotations.media.Schema;

@XmlEnum
@Schema(description = "A description of a hash-algorithm")
public enum HashAlgorithm {
  @XmlEnumValue("SHA-1")
  SHA_1(String.valueOf("SHA-1")),
  @XmlEnumValue("SHA-256")
  SHA_256(String.valueOf("SHA-256")),
  @XmlEnumValue("SHA-384")
  SHA_384(String.valueOf("SHA-384")),
  @XmlEnumValue("SHA-512")
  SHA_512(String.valueOf("SHA-512")),
  @XmlEnumValue("MD5")
  MD5(String.valueOf("MD5"));


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