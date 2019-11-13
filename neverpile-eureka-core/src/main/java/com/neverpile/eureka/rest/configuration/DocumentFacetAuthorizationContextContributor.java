package com.neverpile.eureka.rest.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.neverpile.authorization.api.AuthorizationContext;
import com.neverpile.authorization.api.AuthorizationContextContributor;
import com.neverpile.authorization.policy.impl.CompositeAuthorizationContext;
import com.neverpile.authorization.policy.impl.PrefixAuthorizationContext;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentFacet;

/**
 * A bridge from {@link DocumentFacet}s wanting to contribute to the {@link AuthorizationContext}
 * which does its work by implementing {@link AuthorizationContextContributor}.
 * <p>
 * FIXME: There is a lot of indirection going on here: {@link DocumentFacet} to
 * <code>AuthorizationContextContributor&lt;Document&gt;</code> to {@link AuthorizationContext}.
 * There might be a simpler solution out there.
 */
@Component
public class DocumentFacetAuthorizationContextContributor implements AuthorizationContextContributor<Document> {

  @Autowired(required = false)
  List<DocumentFacet<?>> facets;

  @Override
  public AuthorizationContext contributeAuthorizationContext(final Document source) {
    CompositeAuthorizationContext ctx = new CompositeAuthorizationContext();

    if (null != facets) {
      facets.forEach(f -> {
        AuthorizationContext ac = f.getAuthorizationContextContribution(source);
        if (null != ac)
          ctx.subContext(new PrefixAuthorizationContext(f.getName(), ac));
      });
    }

    return ctx;
  }

}
