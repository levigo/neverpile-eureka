package com.neverpile.eureka.plugin.metadata.rest;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.neverpile.common.authorization.api.HintRegistrations;
import com.neverpile.common.authorization.policy.ResourceHints;
import com.neverpile.eureka.impl.authorization.DefaultDocumentAuthorizationService;

@Component
@ResourceHints
public class MetadataResourceHints implements HintRegistrations {
  @Override
  public List<Hint> getHints() {
    return Arrays.asList( //
        new Hint(DefaultDocumentAuthorizationService.DOCUMENT_RESOURCE + "." + MetadataFacet.NAME, "document.metadata"), //
        new Hint(DefaultDocumentAuthorizationService.DOCUMENT_RESOURCE + "." + MetadataFacet.NAME + ".",
            "document.metadata.element") //
    );
  }
}