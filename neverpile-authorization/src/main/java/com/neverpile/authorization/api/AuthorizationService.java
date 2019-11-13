package com.neverpile.authorization.api;

import java.util.Set;

import org.springframework.boot.web.servlet.server.Session;
import org.springframework.http.HttpRequest;

import com.neverpile.authorization.policy.AccessPolicy;

/**
 * Implementation of this interface are responsible for making and/or delegating authorization
 * decisions related to a requested set of {@link Action}s on some resource. Authorization decisions
 * can additionally be based on arbitrary contextual information supplied by an
 * {@link AuthorizationContext}.
 * <p>
 * Further sources of input for decisions will usually be rights, roles scopes etc. of the principal
 * attempting the access as well as possibly other information like he current {@link HttpRequest},
 * the {@link Session}, configuration information (e.g. an {@link AccessPolicy}) or other
 * factors. However, these sources are not mandated by this interface and must thus be propagated by
 * other means.
 */
public interface AuthorizationService {
  /**
   * Request whether the access specified by the given resource specifier, the set of
   * {@link Action}s within the given {@link AuthorizationContext} shall be permitted or not.
   * 
   * @param resourceSpecifier the resource specifier indicating the targeted resource
   * @param actions the actions that have been requested (or should be checked)
   * @param context the context of the request
   * @return <code>true</code> if the access shall be allowed, <code>false</code> otherwise
   */
  boolean isAccessAllowed(String resourceSpecifier, Set<Action> actions, AuthorizationContext context);
}
