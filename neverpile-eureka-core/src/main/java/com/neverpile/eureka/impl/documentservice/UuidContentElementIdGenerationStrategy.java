package com.neverpile.eureka.impl.documentservice;

import java.util.List;
import java.util.UUID;

import com.neverpile.eureka.api.ContentElementIdGenerationStrategy;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;

/**
 * A {@link ContentElementIdGenerationStrategy} which uses cryptographically strong, random
 * universally unique identifiers (UUIDs) to generate IDs for content elements. IDs are generated
 * using {@link UUID#randomUUID()}.
 * <p>
 * Externally supplied IDs are required to conform to the common UUID representation format. 
 *
 * @see UUID
 */
public class UuidContentElementIdGenerationStrategy implements ContentElementIdGenerationStrategy {

  private static final String UUID_PATTERN = "\\p{XDigit}{8}-(\\p{XDigit}{4}-){3}\\p{XDigit}{12}";

  @Override
  public String createContentId(final List<ContentElement> existingElements, final Digest elementDigest) {
    return UUID.randomUUID().toString();
  }

  @Override
  public boolean validateContentId(final String id, final List<ContentElement> existingElements, final Digest elementDigest) {
    return id.matches(UUID_PATTERN);
  }
}
