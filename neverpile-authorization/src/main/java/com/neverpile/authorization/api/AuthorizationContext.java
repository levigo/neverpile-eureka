package com.neverpile.authorization.api;

import com.neverpile.common.condition.ConditionContext;

/**
 * An {@link AuthorizationContext} provides access to contextual information that can be used in
 * access decisions. The context information is managed as a tree of values addressed by keys which
 * follow the naming conventions used by Java properties, i.e. using dots as level separators.
 */
public interface AuthorizationContext extends ConditionContext {
}
