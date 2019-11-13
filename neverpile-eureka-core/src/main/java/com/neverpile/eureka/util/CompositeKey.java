package com.neverpile.eureka.util;

import java.util.Arrays;
import java.util.Objects;

/**
 * A CompositeKey can be used as a key into a cache, a map etc. which is derived from a set
 * of object passed at construction time. Two {@link CompositeKey}s are equal if and only if all of
 * their components are equal.
 * 
 * Caveat: This class assumes the implementations of the component's {@link #hashCode()} methods to
 * return static, time-invariant values.
 */
public class CompositeKey {
  /**
   * The key components.
   */
  private final Object[] components;

  /**
   * The cached hash code.
   */
  private final int hashCode;

  public CompositeKey(final Object... components) {
    this.components = components;
    hashCode = calcHashCode();
  }

  /**
   * @return the hashCode.
   */
  private int calcHashCode() {
    int h = 932759235;
    for (final Object component : components)
      h ^= Objects.hashCode(component);

    return h;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null || !obj.getClass().equals(getClass()))
      return false;

    final CompositeKey other = (CompositeKey) obj;
    if (other.components.length != components.length)
      return false;

    for (int i = 0; i < components.length; i++)
      if (!Objects.equals(components[i], other.components[i]))
        return false;

    return true;
  }

  @Override
  public String toString() {
    return Arrays.toString(components);
  }
}
