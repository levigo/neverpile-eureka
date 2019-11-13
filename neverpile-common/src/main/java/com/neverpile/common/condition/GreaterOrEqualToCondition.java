package com.neverpile.common.condition;

/**
 * A condition evaluating if the given context value is greater or equal to the corresponding predicate value.
 * <p>
 * For evaluation the common {@link Comparable}  Interface is used. If the value is null or the values cannot
 * be compared in this way false will be returned.
 */
public class GreaterOrEqualToCondition extends ComparisonCondition {
  @SuppressWarnings("unchecked")
  @Override
  protected boolean compare(final Object predicateValue, final Object value) {
    // anything is considered greater than or equal to null
    if(null == predicateValue)
      return true;
    
    if(!(predicateValue instanceof Comparable))
      throw new IllegalArgumentException("The predicate value " + value + " (" + value.getClass() + ") does not implement Comparable");
    
    try {
      return ((Comparable<Object>)predicateValue).compareTo(value) <= 0;
    } catch (Exception e) {
      // Values can not be compared.
      return false;
    }
  }
}
