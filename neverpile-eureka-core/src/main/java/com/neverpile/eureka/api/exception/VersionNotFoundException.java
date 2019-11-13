package com.neverpile.eureka.api.exception;

import com.neverpile.eureka.api.NeverpileException;

public class VersionNotFoundException extends NeverpileException {
  private static final long serialVersionUID = 1L;

  public VersionNotFoundException(final String msg, final String version) {
    super("version: " + version + " : " + msg);
  }
}
