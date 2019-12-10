package com.neverpile.eureka.util;

/**
 * A collection of static utility methods related to threads/threading.
 */
public class Threads {
  public Threads() {
    // just static utility methods around here
  }

  /**
   * Just like {@link Thread#sleep(long)} but without the {@link InterruptedException} in the method
   * signature. If the sleep is interrupted, re-interrupt the thread and bail out with a
   * {@link RuntimeException}.
   * 
   * @param interval time to sleep in ms.
   */
  public static void sleepSafely(final long interval) {
    try {
      Thread.sleep(interval);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while sleeping");
    }
  }
}
