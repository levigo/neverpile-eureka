package com.neverpile.eureka.impl.authorization;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import com.neverpile.authorization.api.Action;
import com.neverpile.authorization.api.AuthorizationContext;
import com.neverpile.authorization.api.AuthorizationContextContributor;
import com.neverpile.authorization.api.AuthorizationService;
import com.neverpile.authorization.policy.impl.CompositeAuthorizationContext;
import com.neverpile.authorization.policy.impl.PrefixAuthorizationContext;
import com.neverpile.eureka.api.DocumentAuthorizationService;
import com.neverpile.eureka.model.Document;

/**
 * A default implementation of {@link DocumentAuthorizationService} which delegates to an injected
 * {@link AuthorizationService}.
 * <p>
 * The {@link AuthorizationContext} is populated by querying all registered
 * {@link AuthorizationContextContributor} for {@link Document}s for contributions.
 */
@Component
@Import(DocumentResourceHints.class)
public class DefaultDocumentAuthorizationService implements DocumentAuthorizationService {
  public static final String DOCUMENT_RESOURCE = "document";

  @Autowired
  AuthorizationService authorizationService;

  @Autowired(required = false)
  List<AuthorizationContextContributor<Document>> contextContributors;

  @Override
  public boolean authorizeSubresourceAction(final Document document, final Action action,
      final String... subResourcePath) {
    return authorizationService.isAccessAllowed(constructResourcePath(subResourcePath), Collections.singleton(action),
        constructAuthorizationContext(document));
  }

  private AuthorizationContext constructAuthorizationContext(final Document document) {
    CompositeAuthorizationContext authContext = new CompositeAuthorizationContext();

    if (null != contextContributors)
      contextContributors.forEach(cc -> authContext.subContext(cc.contributeAuthorizationContext(document)));

    return new PrefixAuthorizationContext("document", authContext);
  }

  private String constructResourcePath(final String... subResourcePath) {
    return DOCUMENT_RESOURCE + "." + String.join(".", subResourcePath);
  }
}
