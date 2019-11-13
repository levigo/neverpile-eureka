package com.neverpile.common.condition;

import com.neverpile.common.specifier.Specifier;

/**
 * A condition implementation which checks for the existence of all target variables. A target
 * variable is considered existent if it resolves to any non-<code>null</code> value.
 */
public class ExistsCondition extends AbstractTargetListCondition<ExistsCondition> {
  @Override
  protected boolean eval(final Specifier s, final Object resolvedValue) {
    return resolvedValue != null;
  }
}
