package com.neverpile.authorization.basic;

import java.util.Set;

import com.neverpile.authorization.api.Action;
import com.neverpile.authorization.api.AuthorizationContext;
import com.neverpile.authorization.api.AuthorizationService;

/**
 * A trivial implementation of {@link AuthorizationService} which allows all access attempts.
 */
public class AllowAllAuthorizationService implements AuthorizationService {
  @Override
  public boolean isAccessAllowed(final String resourceSpecifier, final Set<Action> actions,
      final AuthorizationContext context) {
    return true;
  }
}
