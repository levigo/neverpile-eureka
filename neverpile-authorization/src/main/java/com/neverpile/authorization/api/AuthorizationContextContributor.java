package com.neverpile.authorization.api;

/**
 * {@link AuthorizationContextContributor} can contribute contextual information for authorization
 * checks related to some domain object by providing an implementation of
 * {@link AuthorizationContext}.
 * 
 * @param <D> the type of domain object this contributor deals with 
 */
public interface AuthorizationContextContributor<D> {
  /**
   * Provide contextual information related to the given domain object.
   * 
   * @param source the domain object
   * @return an implementation of {@link AuthorizationContext} providing the information or
   *         <code>null</code> if no such information can be contributed
   */
  AuthorizationContext contributeAuthorizationContext(D source);
}
