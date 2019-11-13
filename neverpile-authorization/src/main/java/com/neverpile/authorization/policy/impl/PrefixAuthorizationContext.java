package com.neverpile.authorization.policy.impl;

import com.neverpile.authorization.api.AuthorizationContext;
import com.neverpile.common.specifier.Specifier;

/**
 * An implementation of {@link AuthorizationContext} which manages a sub-tree of the value space starting with some prefix.
 */
public class PrefixAuthorizationContext implements AuthorizationContext {
  private final Specifier prefix;

  private final AuthorizationContext delegate;

  /**
   * Create a condition context with the given prefix.
   *
   * @param prefix   the prefix with or without a trailing dot
   * @param delegate the delegate
   */
  public PrefixAuthorizationContext(final String prefix, final AuthorizationContext delegate) {
    this.delegate = delegate;
    this.prefix = Specifier.from(prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix);
  }

  @Override
  public Object resolveValue(final Specifier key) {
    if (!key.startsWith(prefix))
      return null;

    return delegate.resolveValue(key.suffix(prefix));
  }

  @Override
  public String toString() {
    return "startsWith: " + prefix;
  }
}
