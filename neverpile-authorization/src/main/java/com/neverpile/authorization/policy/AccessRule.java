package com.neverpile.authorization.policy;

import static java.util.Arrays.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.neverpile.authorization.api.Action;
import com.neverpile.common.condition.AndCondition;
import com.neverpile.common.condition.Condition;

import io.swagger.annotations.ApiModelProperty;

/**
 * Access rules for the basis for authorization decisions. The matching and evaluation process is
 * described in {@link AccessPolicy}.
 */
public class AccessRule {
  public static final String ANY = "*";

  public static final String ANONYMOUS_CALLER = "anonymous";

  public static final String AUTHENTICATED = "authenticated";

  public static final String PRINCIPAL = "principal:";

  public static final String ROLE = "role:";

  @JsonProperty(required = false)
  @ApiModelProperty("A name/description of a rule")
  private String name;

  @ApiModelProperty("The effect to be caused if this rule matches")
  private Effect effect;

  @ApiModelProperty("The subjects matched by this rule. Either 'principal:'s or 'role:'s or 'anonymous'")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> subjects = new ArrayList<>();

  @ApiModelProperty("The resources matched by this rule. (TBD: pointer to possible resources)")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> resources = new ArrayList<>();

  @ApiModelProperty("The actions matched by this rule. (TBD: pointer to possible actions)")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<String> actions = new ArrayList<>();

  @ApiModelProperty("Additional conditions to be satisfied for this rule to match")
  private AndCondition conditions = new AndCondition();

  /**
   * Get the human-readable name of this rule.
   * 
   * @return the human-readable name
   */
  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Return the effect to use when a match of this rule is found.
   * 
   * @return the effect
   */
  public Effect getEffect() {
    return effect;
  }

  public void setEffect(final Effect effect) {
    this.effect = Objects.requireNonNull(effect);
  }

  /**
   * Return the list of subjects (subject-specifier strings) to match against the subject/principal
   * of an authorization request.
   * 
   * @return the list of subjects
   */
  public List<String> getSubjects() {
    return subjects;
  }

  public void setSubjects(final List<String> subjects) {
    this.subjects = Objects.requireNonNull(subjects);
  }

  /**
   * Return the list of resources (resource-specifier strings) to match against the target resource
   * of an authorization request.
   * 
   * @return the list of resources
   */
  public List<String> getResources() {
    return resources;
  }

  public void setResources(final List<String> resources) {
    this.resources = Objects.requireNonNull(resources);
  }

  /**
   * Return the list of action keys to match against the requested actions of an authorization
   * request.
   * 
   * @return the list of action keys
   */
  public List<String> getActions() {
    return actions;
  }

  public void setActions(final List<String> actions) {
    this.actions = Objects.requireNonNull(actions);
  }

  /**
   * Return the tree of conditions to match against the context of an authorization request. The
   * tree's root starts with a (possibly empty and this always <code>true</code>-evaluating)
   * {@link AndCondition}.
   * 
   * @return the list of action keys
   */
  public AndCondition getConditions() {
    return conditions;
  }

  public void setConditions(final AndCondition conditions) {
    this.conditions = Objects.requireNonNull(conditions);
  }

  /**
   * Set the effect and return this rule for builder-style chaining.
   * 
   * @param effect the effect
   * @return this rule
   */
  public AccessRule withEffect(final Effect effect) {
    setEffect(effect);
    return this;
  }

  /**
   * Set subjects and return this rule for builder-style chaining.
   * 
   * @param subjects the subjects
   * @return this rule
   */
  public AccessRule withSubjects(final String... subjects) {
    setSubjects(asList(subjects));
    return this;
  }

  /**
   * Set the resources and return this rule for builder-style chaining.
   * 
   * @param resources the resources
   * @return this rule
   */
  public AccessRule withResources(final String... resources) {
    setResources(asList(resources));
    return this;
  }

  /**
   * Set the name and return this rule for builder-style chaining.
   * 
   * @param name the name
   * @return this rule
   */
  public AccessRule withName(final String name) {
    setName(name);
    return this;
  }

  /**
   * Set the actions and return this rule for builder-style chaining.
   * 
   * @param actions the actions
   * @return this rule
   */
  public AccessRule withActions(final Action... actions) {
    setActions(Stream.of(actions).map(a -> a.key()).collect(Collectors.toList()));
    return this;
  }

  /**
   * Add a condition return this rule for builder-style chaining.
   * @param condition a condition
   * 
   * @return this rule
   */
  public AccessRule withCondition(final Condition condition) {
    conditions.addCondition(condition);
    return this;
  }
}
