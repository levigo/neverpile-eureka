package com.neverpile.common.condition;

/**
 * A condition which evaluates to the logical disjunction (or) of its sub-conditions.
 */
public class OrCondition extends CompositeCondition<OrCondition> {
  @Override
  public boolean matches(final ConditionContext context) {
    return getConditions().stream().anyMatch(c -> c.matches(context));
  }
}
