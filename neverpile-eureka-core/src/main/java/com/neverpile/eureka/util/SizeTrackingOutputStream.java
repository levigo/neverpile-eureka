package com.neverpile.eureka.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A {@link FilterOutputStream} that delegate writes to another stream and tracks the number of
 * bytes written.
 * <p>
 * Thread safety: this stream implementation is not thread safe.
 */
public class SizeTrackingOutputStream extends FilterOutputStream {

  private long written;

  public SizeTrackingOutputStream(final OutputStream out) {
    super(out);
  }

  @Override
  public void write(final int b) throws IOException {
    out.write(b);
    written++;
  }

  @Override
  public void write(final byte[] b) throws IOException {
    out.write(b);
    written += b.length;
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    out.write(b, off, len);
    written += len;
  }

  public long getBytesWritten() {
    return written;
  }
}