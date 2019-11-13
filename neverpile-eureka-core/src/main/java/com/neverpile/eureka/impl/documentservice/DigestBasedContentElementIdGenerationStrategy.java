package com.neverpile.eureka.impl.documentservice;

import java.util.List;

import org.springframework.security.crypto.codec.Hex;

import com.neverpile.eureka.api.ContentElementIdGenerationStrategy;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;

/**
 * A {@link ContentElementIdGenerationStrategy} which uses the content element's {@link Digest} to
 * generate content element IDs. The generated IDs have the form
 * {@code &lt;digest algorithm&gt;_&lt;digest encoded as hexadecimal bytes&gt;}.
 * <p>
 * Externally supplied IDs are required to exactly match the ID that would have been generated.
 */
public class DigestBasedContentElementIdGenerationStrategy implements ContentElementIdGenerationStrategy {
  @Override
  public String createContentId(final List<ContentElement> existingElements, final Digest elementDigest) {
    return elementDigest.getAlgorithm().name() + "_" + new String(Hex.encode(elementDigest.getBytes()));
  }
}
