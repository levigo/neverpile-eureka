package com.neverpile.eureka.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "An enum describing the type of encryption used on an object")
public enum EncryptionType {
  @ApiModelProperty("Objects are with this encryption type are `shared` with the server and thus not encrypted")
  SHARED(String.valueOf("shared")), 

  @ApiModelProperty("Objects are with this encryption type are `private` to the client"
      + " and thus encrypted so that the server does not have access to the content")
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