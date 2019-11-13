package com.neverpile.common.condition;

import com.neverpile.common.specifier.Specifier;

/**
 * An {@link ConditionContext} provides access to contextual information that can be used in
 * access decisions. The context information is managed as a tree of values addressed by keys which
 * follow the naming conventions used by Java properties, i.e. using dots as level separators.
 * <p>
 * The sole responsibility of this interface is the resolution of value keys into context values.
 * The details of this resolution process are implementation-dependent.
 */
public interface ConditionContext {
  /**
   * Return the value associated with the given key or <code>null</code> if the value cannot be
   * resolved.
   *
   * @param key the value key
   * @return the value or <code>null</code>
   */
  Object resolveValue(Specifier key);

  /**
   * Mainly for unit testing: resolve a value from a string-based key instead of a parsed specifier.
   *
   * @param key the value key
   * @return the value or <code>null</code>
   */
  default Object resolveValue(final String key) {
    return resolveValue(Specifier.from(key));
  }
}
