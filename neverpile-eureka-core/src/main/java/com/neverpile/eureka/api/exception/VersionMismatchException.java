package com.neverpile.eureka.api.exception;

import com.neverpile.eureka.api.NeverpileException;

public class VersionMismatchException extends NeverpileException {
  private static final long serialVersionUID = 1L;

  public VersionMismatchException(final String msg,final String expectedVersion,final String actualVersion){
    super("expectedVersion: " + expectedVersion + " actualVersion: " + actualVersion + " : " + msg);
  }
}
