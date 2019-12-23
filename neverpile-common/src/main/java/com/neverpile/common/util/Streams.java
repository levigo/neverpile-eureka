package com.neverpile.common.util;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods related to streams.
 */
public class Streams {
  public Streams() {
    // just static utility methods
  }
  
  /**
   * Create a stream from an iterator.
   * 
   * @param <T> Type of items to be streamed.
   * @param i the iterator
   * @return a stream
   */
  public static <T> Stream<T> of(final Iterator<T> i) {
    return StreamSupport.stream(((Iterable<T>) () -> i).spliterator(), false);
  }
}
