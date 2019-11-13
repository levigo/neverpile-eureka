package com.neverpile.authorization.policy.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.neverpile.authorization.api.AuthorizationContext;
import com.neverpile.common.specifier.Specifier;

/**
 * An implementation of {@link AuthorizationContext} which manages a list of delegate
 * <code>AuthorizationContexts</code> each of which is queried in turn in order to resolve values.
 * The first delegate returning a non-<code>null</code> "wins", that is: its returned value is
 * returned as the composite resolution result.
 */
public class CompositeAuthorizationContext implements AuthorizationContext {
  private final List<AuthorizationContext> subContexts = new ArrayList<>();

  @Override
  public Object resolveValue(final Specifier key) {
    return subContexts.stream().map(c -> c.resolveValue(key)).filter(Objects::nonNull).findFirst().orElse(null);
  }

  /**
   * Add the given sub-context to this context.
   *
   * @param ctx the sub-context
   * @return this instance for fluent addition of multiple sub-contexts
   */
  public CompositeAuthorizationContext subContext(final AuthorizationContext ctx) {
    subContexts.add(ctx);
    return this;
  }
}
