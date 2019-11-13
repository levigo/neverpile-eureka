package com.neverpile.common.condition;

/**
 * A condition which evaluates to the logical negation (not) of its sub-conditions. As all composite
 * conditions can optionally take more than one sub-condition, this is actually implemented as a
 * NAND condition, i.e. it evaluates to <code>true</code> iff there is at least one sub-condition
 * evaluating to <code>false</code>.
 */
public class NotCondition extends CompositeCondition<NotCondition> {
  @Override
  public boolean matches(final ConditionContext context) {
    return !getConditions().stream().allMatch(c -> c.matches(context));
  }
}
