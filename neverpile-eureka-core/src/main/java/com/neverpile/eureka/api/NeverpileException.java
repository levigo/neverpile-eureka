package com.neverpile.eureka.api;

import java.io.Serial;

/**
 * Base class for all neverpile specific exceptions.
 */
public class NeverpileException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public NeverpileException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public NeverpileException(final String message) {
    super(message);
  }

  public NeverpileException(final Throwable cause) {
    super(cause);
  }
}
