package com.neverpile.common.condition;

/**
 * A condition evaluating if the given context value is greater than the corresponding predicate value.
 * <p>
 * For evaluation the common {@link Comparable}  Interface is used. If the value is null or the values cannot
 * be compared in this way false will be returned.
 */
public class GreaterThanCondition extends ComparisonCondition {
  @SuppressWarnings("unchecked")
  @Override
  protected boolean compare(final Object predicateValue, final Object value) {
    // anything non-null is considered greater than null
    if(null == predicateValue)
      return value != null;
    
    if(!(predicateValue instanceof Comparable))
      throw new IllegalArgumentException("The predicate value " + value + " (" + value.getClass() + ") does not implement Comparable");
    
    try {
      return ((Comparable<Object>)predicateValue).compareTo(value) < 0;
    } catch (Exception e) {
      // Values can not be compared.
      return false;
    }
  }
}
