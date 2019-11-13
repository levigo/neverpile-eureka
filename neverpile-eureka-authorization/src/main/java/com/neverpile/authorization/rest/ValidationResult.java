package com.neverpile.authorization.rest;

public class ValidationResult {
  public enum Type {
    INFO, WARNING, ERROR,
  }

  private Type type;

  private String message;

  public ValidationResult() {
  }
  
  public ValidationResult(final Type type, final String message) {
    super();
    this.type = type;
    this.message = message;
  }

  public Type getType() {
    return type;
  }

  public void setType(final Type type) {
    this.type = type;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }
}
