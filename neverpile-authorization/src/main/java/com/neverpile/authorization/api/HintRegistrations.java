package com.neverpile.authorization.api;

import java.util.List;

/**
 * Implementations of this interface are responsible for providing {@link Hint}s.
 */
public interface HintRegistrations {
  /**
   * Hints can be used by a user interface to provide, well, hints as to what values can be used in
   * configuration fields.
   */
  public class Hint {
    private final String prefix;

    private final String documentationKey;

    public Hint(final String prefix, final String documentationKey) {
      super();
      this.prefix = prefix;
      this.documentationKey = documentationKey;
    }

    public String getPrefix() {
      return prefix;
    }

    public String getDocumentationKey() {
      return documentationKey;
    }
  }

  List<Hint> getHints();
}
