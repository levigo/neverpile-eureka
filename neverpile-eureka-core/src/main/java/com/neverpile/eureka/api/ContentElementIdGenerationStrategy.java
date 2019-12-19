package com.neverpile.eureka.api;

import java.util.List;

import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;

/**
 * {@link ContentElement}s must have unique IDs within a {@link com.neverpile.eureka.model.Document}. This interfaces
 * defines functions to create and validate such IDs with a implementation of choice.
 */
public interface ContentElementIdGenerationStrategy {

  /**
   * Create a new {@link ContentElement#id} for a {@link ContentElement} which has to be unique among all other {@link ContentElement}s of the
   * same {@link com.neverpile.eureka.model.Document}.
   *
   * @param existingElements Already existing {@link ContentElement}s within the document the new ID is generated for.
   *                         Excluding the {@link ContentElement} the ID will be generated for.
   * @param elementDigest    An digest of the {@link ContentElement} a new ID is generated for.
   * @return String representation of the newly generated ID.
   */
  String createContentId(List<ContentElement> existingElements, Digest elementDigest);

  /**
   * Validates an existing {@link ContentElement#id}.
   *
   * @param id               The {@link ContentElement#id} String to be validated.
   * @param existingElements Already existing {@link ContentElement}s within the document the new ID is generated for.
   *                         Excluding the {@link ContentElement} to be validated.
   * @param elementDigest    An digest of the {@link ContentElement} for which the ID should be validated..
   * @return {@code true} if the ID is valid. {@code false} otherwise.
   */
  default boolean validateContentId(final String id, final List<ContentElement> existingElements,
      final Digest elementDigest) {
    // accept only what we'd be generating
    return id.matches(createContentId(existingElements, elementDigest));
  }

}
