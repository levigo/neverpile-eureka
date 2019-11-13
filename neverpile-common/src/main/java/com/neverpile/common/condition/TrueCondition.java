package com.neverpile.common.condition;

import com.neverpile.common.specifier.Specifier;

/**
 * A condition implementation which checks whether the target variables is True. A target variable
 * is considered true if it resolves to a {@link Boolean} with the value <code>true</code> or the
 * {@link #toString()} representation of the value is considered <code>true</code> by
 * {@link Boolean#valueOf(String)} which it does if the string is equal to "true" ignoring the case.
 * Null variables are considered <code>false</code>.
 */
public class TrueCondition extends AbstractTargetListCondition<TrueCondition> {
  @Override
  protected boolean eval(final Specifier s, final Object value) {
    return null == value ?
        false :
        value instanceof Boolean ? ((Boolean) value) : Boolean.valueOf(value.toString().trim());
  }
}
