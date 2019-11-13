package com.neverpile.common.condition;

/**
 * A condition which evaluates to the logical conjunction (and) of its sub-conditions.
 */
public class AndCondition extends CompositeCondition<AndCondition> {
  @Override
  public boolean matches(final ConditionContext context) {
    return getConditions().stream().allMatch(c -> c.matches(context));
  }
}
