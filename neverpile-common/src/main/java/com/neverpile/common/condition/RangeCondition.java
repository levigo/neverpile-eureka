package com.neverpile.common.condition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.neverpile.common.specifier.Specifier;

/**
 * A condition evaluating if the given context value is in between the corresponding predicate values.
 * <p>
 * For evaluation the common {@link Comparable}  Interface is used. If the value is null or the values cannot
 * be compared in this way false will be returned.
 */
public class RangeCondition extends Condition {
  private final Map<Specifier, List<Comparable<?>>> predicates = new HashMap<>();

  /**
   * Add a predicate for the given value key matched against the range between the first and the second given value.
   *
   * @param target the target value key
   * @param values exactly two values to build a range between the first and second value
   */
  @JsonAnySetter
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  public void addPredicate(final String target, final List<Comparable<?>> values) {
    predicates.put(Specifier.from(target), values);
  }

  /**
   * Get current predicates with specifiers and values.
   *
   * @return predicate map
   */
  @JsonAnyGetter
  public Map<Specifier, List<Comparable<?>>> getPredicates() {
    return predicates;
  }

  @SuppressWarnings({
      "rawtypes", "unchecked"
  })
  @Override
  public boolean matches(final ConditionContext context) {
    return predicates.entrySet().stream().allMatch(e -> {
      Comparable<?> value = (Comparable<?>) context.resolveValue(e.getKey());

      List<Comparable<?>> predicateValue = e.getValue();
      
      // We need to treat a null-predicate like a list with a single null-element
      // due to JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY.
      return null != predicateValue && predicateValue.size() == 2 && ((Comparable)predicateValue.get(0)).compareTo(value) <= 0
          && ((Comparable)predicateValue.get(1)).compareTo(value) >= 0;
    });
  }

  /**
   * Add a predicate and return this condition for builder-style chaining.
   *
   * @param target the target value key
   * @param values the possible values considered for equality
   * @return this condition
   */
  public RangeCondition withPredicate(final String target, final List<Comparable<?>> values) {
    predicates.put(Specifier.from(target), values);
    return this;
  }
}
