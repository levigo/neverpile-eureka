package com.neverpile.eureka.api.exception;

import java.io.Serial;

import com.neverpile.eureka.api.NeverpileException;

public class VersionNotFoundException extends NeverpileException {
  @Serial
  private static final long serialVersionUID = 1L;

  public VersionNotFoundException(final String msg, final String version) {
    super("version: " + version + " : " + msg);
  }
}
