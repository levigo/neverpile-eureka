package com.neverpile.common.condition;

import java.util.Objects;

/**
 * A condition evaluating equality between predicate value and its given context counterpart.
 * <p>
 * Equality is tested using {@link Objects#equals(Object)}, thus all JSON-supported value types can
 * be used, however, care must be taken to use the correct type, as no type conversion is attempted.
 * Thus, "1" (the string) is not considered equal to 1 (the integer).
 */
public class EqualsCondition extends ComparisonCondition {
  public static ComparisonCondition eq(final String target, final Object value) {
    return new EqualsCondition().withPredicate(target, value);
  }

  @Override
  protected boolean compare(final Object predicateValue, final Object value) {
    return null == predicateValue ? Objects.isNull(value) : Objects.equals(value, predicateValue);
  }
}
