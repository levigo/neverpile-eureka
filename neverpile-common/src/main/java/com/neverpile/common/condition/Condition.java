package com.neverpile.common.condition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

/**
 * Conditions allow an almost arbitrarily complex decisions based on an {@link ConditionContext}
 * which is responsible for supplying context information through named context variables.
 * <p>
 * This class is the base for all condition implementations. All conditions have an optional
 * {@link #name} which can be used in administrative GUIs, for logging or debugging.
 */
public abstract class Condition {
  private String name;

  public Condition() {
  }

  /**
   * Return the human-readable name of this condition.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  @JsonProperty(required = false)
  @JsonInclude(Include.NON_NULL)
  @ApiModelProperty("A name/description of a condition")
  public void setName(final String description) {
    this.name = description;
  }

  /**
   * Return whether this condition matches the supplied context.
   *
   * @param context the authorization context used to resolve context variables
   * @return <code>true</code> if the conditions matches
   */
  public abstract boolean matches(ConditionContext context);
}
