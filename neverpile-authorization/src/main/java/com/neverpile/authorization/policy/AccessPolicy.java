package com.neverpile.authorization.policy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An {@link AccessPolicy} describes the actions that are permitted within a system through
 * {@link AccessRule}s that are matched against an authorization request and the authorization
 * {@link Effect} they mandate.
 * <p>
 * The matching process works as follows:
 * <ul>
 * <li>The provided {@link #rules} are matched - in the ordering given by the list and thus the
 * ordering in which they appear in the JSON file - against authorization requests.
 * <li>Rules are matched using the targeted resource(s) ({@link AccessRule#getResources()}), the
 * requesting subject(s) ({@link AccessRule#getSubjects()}), the requested action(s)
 * ({@link AccessRule#getActions()}) and, optionally, other conditions
 * ({@link AccessRule#getConditions()}).
 * <li>The authorization effect ({@link AccessRule#getEffect()}) of the <em>first</em> matching rule
 * decides about the outcome of the authorization request.
 * <li>If no rule matches, the default effect ({@link #getDefaultEffect()}) is used.
 * </ul>
 * <p>
 * An access policy has a date ({@link #getValidFrom()}) that indicates the moment at which it
 * becomes applicable, but no date that indicates the moment at which it loses applicability.
 * Instead, once a policy becomes applicable, it remains applicable until superseded by a policy
 * with a later date. Thus, if more than one policy is defined then the one with the most recent
 * date is currently applicable.
 * <p>
 * A policy has an optional description ({@link #getDescription()}) which can be used in GUI
 * editors, log output etc. to describe the policy in human-readable form.
 */
@Schema(description = "An access policy descibes access rights users can exercise within the system")
public class AccessPolicy implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final String VERISON_1 = "2018-09-26";

  @Schema(description = "The version of this policy's schema. There is currently only one valid version: '" + VERISON_1
      + "'. Policies using other versions may not be compatible.")
  private String _version = VERISON_1;

  @Schema(description = "The timestamp specifying the date and time at which this policy becomes "
      + "valid (unless replaced by a policy with a later timestamp)")
  @JsonProperty(value = "valid_from", required = false)
  @JsonAlias("validFrom")
  @JsonInclude(Include.NON_NULL)
  private Date validFrom;

  @Schema(description = "A description of this access policy")
  private String description;

  @JsonProperty("default_effect")
  @Schema(description = "The default effect of this policy when no access rule matched")
  private Effect defaultEffect = Effect.DENY;

  @Schema(description = "The list of access rules ")
  private List<AccessRule> rules = new ArrayList<>();

  /**
   * Return the start date of the policy's validity period.
   * 
   * @return the start date
   */
  public Date getValidFrom() {
    return validFrom;
  }

  public void setValidFrom(final Date validFrom) {
    this.validFrom = Objects.requireNonNull(validFrom);
  }

  /**
   * Return the human-readable description (or name) of the policy
   * 
   * @return a description or <code>null</code>
   */
  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  /**
   * Get the list of rules which make up this policy.
   * 
   * @return the list of rules
   */
  public List<AccessRule> getRules() {
    return rules;
  }

  public void setRules(final List<AccessRule> rules) {
    this.rules = Objects.requireNonNull(rules);
  }

  /**
   * Return the default effect to be applied if no {@link AccessRule} matched an authorization
   * effect.
   * 
   * @return the default effect
   */
  public Effect getDefaultEffect() {
    return defaultEffect;
  }

  public void setDefaultEffect(final Effect defaultEffect) {
    this.defaultEffect = Objects.requireNonNull(defaultEffect);
  }

  /**
   * Add a rule and return this policy for builder-style chaining.
   * 
   * @param r the rule
   * @return this policy
   */
  public AccessPolicy withRule(final AccessRule r) {
    rules.add(r);
    return this;
  }

  /**
   * Set the default effect and return this policy for builder-style chaining.
   * 
   * @param effect the default effect
   * @return this policy
   */
  public AccessPolicy withDefaultEffect(final Effect effect) {
    setDefaultEffect(effect);
    return this;
  }

  /**
   * Set the start date of the validity period and return this policy for builder-style chaining.
   * 
   * @param from the start date
   * @return this policy
   */
  public AccessPolicy withValidFrom(final Date from) {
    setValidFrom(from);
    return this;
  }

  /**
   * Set the human-readable description (or name) and return this policy for builder-style chaining.
   * 
   * @param description the start date
   * @return this policy
   */
  public AccessPolicy withDescription(final String description) {
    setDescription(description);
    return this;
  }

  public String get_version() {
    return _version;
  }

  public void set_version(final String _version) {
    this._version = _version;
  }

}
