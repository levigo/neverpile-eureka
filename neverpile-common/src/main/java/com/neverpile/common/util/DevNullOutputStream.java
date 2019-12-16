package com.neverpile.common.util;

import java.io.OutputStream;

/**
 * An {@link OutputStream} that discards everything it reads
 */
public class DevNullOutputStream extends OutputStream {
  @Override
  public void write(final int b) {
    // nothing to do
  }
}