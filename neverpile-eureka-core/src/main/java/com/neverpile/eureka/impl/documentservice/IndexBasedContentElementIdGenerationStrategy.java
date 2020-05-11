package com.neverpile.eureka.impl.documentservice;

import java.util.List;

import org.springframework.util.comparator.Comparators;

import com.neverpile.eureka.api.ContentElementIdGenerationStrategy;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Digest;

/**
 * A {@link ContentElementIdGenerationStrategy} which numbers the content elements in the order they
 * have been added. The first element will be assigned id {@code 0} the second {@code 1} and so on.
 * Existing IDs not matching this format (due to a change in the generation strategy) are ignored
 * during the generation phase.
 * <p>
 * Externally supplied IDs are required to exactly match the ID that would have been generated.
 */
public class IndexBasedContentElementIdGenerationStrategy implements ContentElementIdGenerationStrategy {

  @Override
  public String createContentId(final List<ContentElement> existingElements, final Digest elementDigest) {
    return Long.toString(existingElements.stream().map(e -> {
      // parse existing ids as longs - return -1 for non-numeric ids
      try {
        return Long.parseLong(e.getId());
      } catch (NumberFormatException e1) {
        return -1;
      }
    }).max(Comparators.comparable()).orElse(-1L).longValue() + 1);
  }

  @Override
  public boolean validateContentId(String id, List<ContentElement> existingElements, Digest elementDigest) {
    return existingElements.stream().noneMatch(contentElement -> contentElement.getId().equals(id))
        && id.matches("\\d*");
  }
}
