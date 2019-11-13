package com.neverpile.eureka.impl.authorization;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.neverpile.authorization.api.HintRegistrations;
import com.neverpile.authorization.policy.ResourceHints;

@Component
@ResourceHints
public class DocumentResourceHints implements HintRegistrations {
  @Override
  public List<Hint> getHints() {
    return Arrays.asList( //
        new Hint(DefaultDocumentAuthorizationService.DOCUMENT_RESOURCE, "document"), //
        new Hint(DefaultDocumentAuthorizationService.DOCUMENT_RESOURCE + ".", "document-subresource") //
    );
  }
}