package com.neverpile.eureka.api;

import java.util.List;

import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;

public interface ContentElementIdGenerationStrategy {

  String createContentId(List<ContentElement> existingElements, Digest elementDigest);

  default boolean validateContentId(final String id, final List<ContentElement> existingElements, final Digest elementDigest) {
    // accept only what we'd be generating
    return id.matches(createContentId(existingElements, elementDigest));
  }

}
