package com.neverpile.eureka.rest.api.document;

import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/*
 * Lifted from
 * https://github.com/hamcrest/JavaHamcrest/blob/master/hamcrest-library/src/main/java/org/hamcrest/
 * text/MatchesPattern.java which is in hamcrest 2.0.0.0 only.
 */
public class MatchesPattern extends TypeSafeMatcher<String> {
  private final Pattern pattern;

  public MatchesPattern(final Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  protected boolean matchesSafely(final String item) {
    return pattern.matcher(item).matches();
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("a string matching the pattern '" + pattern + "'");
  }

  /**
   * Creates a matcher of {@link java.lang.String} that matches when the examined string exactly
   * matches the given {@link java.util.regex.Pattern}.
   * 
   * @param pattern the pattern to match
   * @return the corresponding {@link Matcher}
   */
  public static Matcher<String> matchesPattern(final Pattern pattern) {
    return new MatchesPattern(pattern);
  }

  /**
   * Creates a matcher of {@link java.lang.String} that matches when the examined string exactly
   * matches the given regular expression, treated as a {@link java.util.regex.Pattern}.
   * 
   * @param regex the pattern to match
   * @return the corresponding {@link Matcher}
   */
  public static Matcher<String> matchesPattern(final String regex) {
    return new MatchesPattern(Pattern.compile(regex));
  }
}