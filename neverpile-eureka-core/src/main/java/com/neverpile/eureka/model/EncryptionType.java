package com.neverpile.eureka.model;


public enum EncryptionType {
  SHARED(String.valueOf("shared")),

  PRIVATE(String.valueOf("private"));

  private final String value;

  EncryptionType(final String v) {
    value = v;
  }

  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static EncryptionType fromValue(final String v) {
    for (EncryptionType b : EncryptionType.values()) {
      if (String.valueOf(b.value).equals(v)) {
        return b;
      }
    }
    return null;
  }
}