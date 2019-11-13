package com.neverpile.eureka.api;

public class NeverpileException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public NeverpileException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public NeverpileException(final String message) {
    super(message);
  }
}
