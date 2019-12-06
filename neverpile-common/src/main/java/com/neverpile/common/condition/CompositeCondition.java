package com.neverpile.common.condition;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * An abstract condition which connects a set of sub-conditions using some connective operator.
 *
 * @param <C> the type of condition implementation
 */
public abstract class CompositeCondition<C extends CompositeCondition<C>> extends Condition {
  /**
   * The list of sub-conditions.
   */
  private final List<Condition> conditions = new ArrayList<>();

  /**
   * Add the given sub-condition.
   *
   * @param condition the condition
   */
  @JsonIgnore
  public void addCondition(final Condition condition) {
    getConditions().add(condition);
  }

  /**
   * Return the list of sub-conditions.
   *
   * @return the list of sub-conditions
   */
  @JsonIgnore
  public List<Condition> getConditions() {
    return conditions;
  }

  /**
   * Add a sub-condition and return this condition for builder-style chaining.
   *
   * @param condition the condition
   * @return this condition
   */
  @SuppressWarnings("unchecked")
  public C withCondition(final Condition condition) {
    getConditions().add(condition);
    return (C) this;
  }

}