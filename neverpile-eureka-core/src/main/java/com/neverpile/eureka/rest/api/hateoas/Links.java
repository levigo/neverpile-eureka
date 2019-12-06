package com.neverpile.eureka.rest.api.hateoas;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;

import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentFacet;
import com.neverpile.eureka.rest.api.document.content.ContentElementResource;

public class Links {
  private Links() {
  }

  public static Link facet(final Document document, final DocumentFacet<?> facet) {
    return doc(document).slash(facet.getName()).withSelfRel();
  }
  
  public static Link facet(final Document document, final DocumentFacet<?> facet, final String id) {
    return doc(document).slash(facet.getName()).slash(id).withRel(facet.getName());
  }

  public static ControllerLinkBuilder doc(final Document document) {
    return linkTo(ContentElementResource.class).slash(document.getDocumentId());
  }
}
