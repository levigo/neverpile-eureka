package com.neverpile.authorization.policy.impl;

import java.util.Objects;

import com.neverpile.authorization.api.AuthorizationContext;
import com.neverpile.common.specifier.Specifier;

/**
 * An implementation of {@link AuthorizationContext} which resolves just a single value based on a
 * fixed key.
 */
public class SingleValueAuthorizationContext implements AuthorizationContext {
  private final Specifier key;

  private final Object value;

  /**
   * Create a context resolving the given value under the given key.
   *
   * @param key   the key
   * @param value the value
   */
  public SingleValueAuthorizationContext(final String key, final Object value) {
    this.key = Specifier.from(Objects.requireNonNull(key));
    this.value = value;
  }

  /**
   * Create a condition context with the empty key ("").
   *
   * @param value the value
   */
  public SingleValueAuthorizationContext(final Object value) {
    this("", value);
  }

  @Override
  public Object resolveValue(final Specifier key) {
    if (!this.key.equals(key))
      return null;

    return value;
  }
}
