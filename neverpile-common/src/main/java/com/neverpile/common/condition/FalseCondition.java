package com.neverpile.common.condition;

import com.neverpile.common.specifier.Specifier;

/**
 * A condition implementation which checks whether the target variables is False. A target variable
 * is considered false if it resolves to a {@link Boolean} with the value <code>false</code> or the
 * {@link #toString()} representation of the value is considered <code>false</code> by
 * {@link Boolean#valueOf(String)} which it does if the string is <em>not</em> equal to "true"
 * ignoring the case. Null variables are also considered to be <code>false</code>.
 */
public class FalseCondition extends AbstractTargetListCondition<FalseCondition> {
  @Override
  protected boolean eval(final Specifier s, final Object value) {
    return null == value || (value instanceof Boolean ? !((Boolean) value) : !Boolean.valueOf(value.toString().trim()));
  }
}
