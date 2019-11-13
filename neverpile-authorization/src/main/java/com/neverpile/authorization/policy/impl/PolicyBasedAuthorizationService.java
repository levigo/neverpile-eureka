package com.neverpile.authorization.policy.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import com.neverpile.authorization.api.Action;
import com.neverpile.authorization.api.AuthorizationContext;
import com.neverpile.authorization.api.AuthorizationService;
import com.neverpile.authorization.policy.AccessPolicy;
import com.neverpile.authorization.policy.AccessRule;
import com.neverpile.authorization.policy.Effect;
import com.neverpile.authorization.policy.PolicyRepository;
import com.neverpile.common.condition.CoreConditionRegistry;
import com.neverpile.common.condition.config.ConditionModule;

/**
 *
 */
@Component
@Import({CoreConditionRegistry.class, ConditionModule.class})
public class PolicyBasedAuthorizationService implements AuthorizationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyBasedAuthorizationService.class);

  @Autowired
  private PolicyRepository policyRepository;

  @Override
  public boolean isAccessAllowed(final String resourceSpecifier, final Set<Action> actions,
      final AuthorizationContext context) {
    AccessPolicy policy = policyRepository.getCurrentPolicy();

    return isAccessAllowed(resourceSpecifier, actions, context, policy);
  }

  /**
   * Request whether the access specified by the given resource specifier, the set of
   * {@link Action}s within the given {@link AuthorizationContext} shall be permitted or not by the given policy.
   *
   * @param resourceSpecifier the resource specifier indicating the targeted resource
   * @param actions           the actions that have been requested (or should be checked)
   * @param context           the context of the request
   * @param policy            the policy to use for the access control decision
   * @return <code>true</code> if the access shall be allowed, <code>false</code> otherwise
   * @see #isAccessAllowed(String, Set, AuthorizationContext)
   */
  public boolean isAccessAllowed(final String resourceSpecifier, final Set<Action> actions,
      final AuthorizationContext context, final AccessPolicy policy) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    Effect e = policy.getRules().stream() //
        .filter(authentication.isAuthenticated() //
            ? (r) -> matchesAuthenticatedUser(r, authentication) //
            : this::matchesAnonymousUser) //
        .filter(r -> matchesResource(r, resourceSpecifier)) //
        .filter(r -> matchesActions(r, actions)) //
        .filter(r -> satisfiesConditions(r, actions, context)) //
        .map(AccessRule::getEffect) //
        .findFirst() //
        .orElse(policy.getDefaultEffect());

    LOGGER.debug("Authorization for {} on {} with principal {}: {}", actions, resourceSpecifier,
        authentication.isAuthenticated() ? authentication.getPrincipal() : "anonymous", e);

    return e == Effect.ALLOW;
  }

  private boolean matchesAuthenticatedUser(final AccessRule rule, final Authentication authentication) {
    List<String> subjects = rule.getSubjects();

    if (subjects.contains(AccessRule.ANY) || subjects.contains(AccessRule.AUTHENTICATED)) {
      LOGGER.debug("  Rule '{}' matches any/any authenticated user", rule.getName());
      return true;
    }

    if (subjects.contains(AccessRule.PRINCIPAL + authentication.getName())) {
      LOGGER.debug("  Rule '{}' matches the authenticated principal {}", rule.getName(), authentication.getName());
      return true;
    }

    boolean m = authentication.getAuthorities().stream() //
        .anyMatch(a -> subjects.contains(AccessRule.ROLE + a.getAuthority()));

    LOGGER.debug("  Rule '{}' {} the authenticated principal {} with authorities", rule.getName(),
        m ? "matches" : "does not match", authentication.getName(),
        authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(",")));

    return m;
  }

  private boolean matchesAnonymousUser(final AccessRule rule) {
    boolean m = rule.getSubjects().contains(AccessRule.ANY) || rule.getSubjects().contains(AccessRule.ANONYMOUS_CALLER);

    LOGGER.debug("  Rule '{}' {} the anonymous user", rule.getName(), m ? "matches" : "does not match");

    return m;
  }

  /**
   * <p>
   * An access rule matches a resource specifier if any of the resource patterns in the rule match
   * the given specifier. Matching is performed ant-style, but using periods (".") as the path
   * separator.
   *
   * <h3>Examples</h3>
   * <ul>
   * <li>{@code document.metadata.ba?} &mdash; matches {@code document.metadata.bar} but also
   * {@code document.metadata.baz}</li>
   * <li>{@code document.metadata.*-claims} &mdash; matches all {@code metadata} elements with names
   * ending with {@code -claims}</li>
   * <li>{@code document.**} &mdash; matches all sub-resources of documents</li>
   * </ul>
   *
   * @param rule              for which to test the match
   * @param resourceSpecifier the resource specificer to match
   * @return <code>true</code> if a match was found
   * @See {@link AntPathMatcher}
   */
  private boolean matchesResource(final AccessRule rule, final String resourceSpecifier) {
    boolean m = rule.getResources().stream().anyMatch(r -> matchesResource(r, resourceSpecifier));

    LOGGER.debug("  Rule '{}' {} the resource {}", rule.getName(), m ? "matches" : "does not match", resourceSpecifier);

    return m;
  }

  /**
   * An ant-style matcher using periods as path separators.
   */
  private final AntPathMatcher resourcePatternMatcher = new AntPathMatcher(".");

  private boolean matchesResource(final String resourcePattern, final String resourceSpecifier) {
    return resourcePatternMatcher.match(resourcePattern + ".**", resourceSpecifier);
  }

  private boolean matchesActions(final AccessRule rule, final Set<Action> actions) {
    boolean m = actions.stream().allMatch(a -> rule.getActions().contains(a.key())) || rule.getActions().contains("*");

    LOGGER.debug("  Rule '{}' {} the actions ", rule.getName(), m ? "matches" : "does not match", actions);

    return m;
  }

  private boolean satisfiesConditions(final AccessRule rule, final Set<Action> actions,
      final AuthorizationContext conditionContext) {
    boolean m = rule.getConditions().matches(conditionContext);

    LOGGER.debug("  Rule '{}' the context {} the conditions ", rule.getName(), m ? "satisfies" : "does not satisfy",
        actions);

    return m;
  }
}
