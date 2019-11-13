package com.neverpile.common.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.neverpile.common.specifier.Specifier;


/**
 * An abstract condition which uses a list of targets named "target". Implements a default
 * {@link #matches(ConditionContext)} evaluation against each target through
 * {@link #eval(Specifier, Object)}.
 *
 * @param <C> the concrete implementation
 */
public abstract class AbstractTargetListCondition<C extends AbstractTargetListCondition<C>> extends Condition {

  private List<String> targets = new ArrayList<>();
  protected List<Specifier> specifier = new ArrayList<>();

  public AbstractTargetListCondition() {
    super();
  }

  /**
   * Return the list of targets of this condition, i.e. a list of context variable names.
   *
   * @return the list of targets
   */
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @JsonAlias("target")
  public List<String> getTargets() {
    return Collections.unmodifiableList(targets);
  }

  public void setTargets(final List<String> targets) {
    this.targets = targets;
    refreshCache();
  }

  private void refreshCache() {
    this.specifier = targets.stream().map(Specifier::from).collect(Collectors.toList());
  }

  /**
   * Add a target and return this condition for builder-style chaining.
   *
   * @param target the target
   * @return this condition
   */
  @SuppressWarnings("unchecked")
  public C withTarget(final String target) {
    this.targets.add(target);
    refreshCache();
    return (C) this;
  }

  public void withTarget(final int index, final String target) {
    this.targets.set(index, target);
    refreshCache();
  }

  @Override
  public boolean matches(final ConditionContext context) {
    return specifier.stream().allMatch(s -> eval(s, context.resolveValue(s)));
  }

  protected abstract boolean eval(final Specifier s, final Object resolvedValue);
}