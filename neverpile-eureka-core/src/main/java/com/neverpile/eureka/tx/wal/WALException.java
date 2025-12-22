package com.neverpile.eureka.tx.wal;

import java.io.Serial;

import com.neverpile.eureka.api.NeverpileException;

public class WALException extends NeverpileException {
  @Serial
  private static final long serialVersionUID = 1L;

  public WALException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public WALException(final String message) {
    super(message);
  }

}
