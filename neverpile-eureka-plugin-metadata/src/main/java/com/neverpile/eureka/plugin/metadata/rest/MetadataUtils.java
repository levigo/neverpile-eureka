package com.neverpile.eureka.plugin.metadata.rest;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.*;

import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentDto;

class MetadataUtils {

  static void linkify(final Document document, final DocumentDto dto) {
    dto.add(linkTo(MetadataResource.class, document.getDocumentId()).withRel("metadata"));
  }

  static void linkify(final Document document, final MetadataDto mdDto) {
    mdDto.getElements().forEach((k, e) -> {
      linkify(document, k, e);
    });
    mdDto.add(linkTo(MetadataResource.class, document.getDocumentId()).withSelfRel());
  }

  static void linkify(final Document document, final String name, final MetadataElementDto element) {
    element.add(linkTo(MetadataResource.class, document.getDocumentId()).slash(name).withSelfRel());
  }

}
