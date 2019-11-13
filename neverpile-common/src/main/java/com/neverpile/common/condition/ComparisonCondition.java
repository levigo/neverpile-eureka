package com.neverpile.common.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.neverpile.common.specifier.Specifier;

/**
 * Comparison Conditions is a finite Condition and compares one ore more predicates in a given
 * context against one ore more predefined comparison values. The evaluation of the comparison is
 * defined by its implementation classes.
 */
public abstract class ComparisonCondition extends Condition {
  private final Map<Specifier, List<Object>> predicates = new HashMap<>();

  /**
   * Resolution of the comparison between the predicate value and the context Value.
   *
   * @param predicateValue predefined Value to compare against
   * @param contextValue context value to compare.
   * @return result of comparison in dependent on comparison criteria
   */
  protected abstract boolean compare(Object predicateValue, Object contextValue);

  @Override
  public boolean matches(final ConditionContext context) {
    return predicates.entrySet().stream().allMatch(
        e -> e.getValue().stream().anyMatch(v -> compare(v, context.resolveValue(e.getKey())))
    );
  }

  /**
   * Add a predicate for the given value key matched for equality against the given value.
   *
   * @param target the target key
   * @param value the value to compare against
   */
  public void addPredicate(final String target, final Comparable<?> value) {
    predicates.merge(Specifier.from(target), Collections.singletonList(value), (v1, v2) -> {
      v1.addAll(v2);
      return v1;
    });
  }

  /**
   * Add a predicate for the given value key matched for equality against the given values.
   *
   * @param target the target value key
   * @param values the possible values considered for equality
   */
  @JsonAnySetter
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  public void addPredicate(final String target, final List<String> values) {
    predicates.merge(Specifier.from(target), new ArrayList<>(values), (v1, v2) -> {
      v1.addAll(v2);
      return v1;
    });
  }

  /**
   * Get current predicates with specifiers and values.
   *
   * @return predicate map
   */
  @JsonAnyGetter
  public Map<Specifier, List<Object>> getPredicates() {
    return predicates;
  }

  /**
   * Add a predicate and return this condition for builder-style chaining.
   *
   * @param target the target value key
   * @param value the value to compare to
   * @return this condition
   */
  public ComparisonCondition withPredicate(final String target, final Object value) {
    predicates.merge(Specifier.from(target), Collections.singletonList(value), (v1, v2) -> {
      v1.addAll(v2);
      return v1;
    });
    return this;
  }

}
