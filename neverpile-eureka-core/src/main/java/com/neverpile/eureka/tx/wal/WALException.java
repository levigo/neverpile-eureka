package com.neverpile.eureka.tx.wal;

public class WALException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public WALException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public WALException(final String message) {
    super(message);
  }

}
