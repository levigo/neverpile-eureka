package com.neverpile.common.specifier;

import static java.lang.System.arraycopy;
import static java.util.Arrays.copyOfRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.neverpile.common.util.VisibleForTesting;

/**
 * Specifiers are a parsed form of the keys used to address authorization targets and context
 * values. The keys use a hierarchical name structure separated by dots, similar to the convention
 * used for {@link Properties}-keys. Leading or trailing whitespace is removed during parsing. Most
 * Unicode characters are supported, but control characters (including newlines, tabs etc.) are not.
 * <p>
 * The following, simple escaping rules apply:
 * <ul>
 * <li>Dots in expression elements can be escaped using a backslash character: '\.'
 * <li>Backslash-characters themselves can be escaped using a double-backslash: '\\'
 * </ul>
 */
public class Specifier {
  private static final Specifier EMPTY = new Specifier(new String[0], 0);

  /**
   * Parse a specifier from the given string representation.
   *
   * @param stringRepresentation the string representation as described in the class comment
   *          {@link Specifier}.
   * @return the parsed specifier
   */
  public static Specifier from(String stringRepresentation) {
    stringRepresentation = stringRepresentation.trim();

    if (stringRepresentation.isEmpty())
      return EMPTY;

    List<String> parts = new ArrayList<>();

    // ok, so this is the parser to replace the impossible regex-based parser
    StringBuffer sb = new StringBuffer(stringRepresentation.length());
    for (int i = 0; i < stringRepresentation.length(); i++) {
      char c = stringRepresentation.charAt(i);

      // reject control characters
      if (c < ' ')
        throw new IllegalArgumentException(
            "Invalid path '" + stringRepresentation + "': illegal control character at " + i);

      switch (c){
        case '\\' :
          if (++i >= stringRepresentation.length()) {
            throw new IllegalArgumentException(
                "Invalid path '" + stringRepresentation + "': incomplete escape sequence at " + i);
          }

          c = stringRepresentation.charAt(i);
          switch (c){
            case '.' : // escaped dot
            case '\\' : // escaped backslash
              sb.append(c);
              break;

            default :
              throw new IllegalArgumentException(
                  "Invalid path '" + stringRepresentation + "': illegal escape sequence at " + i);
          }
          break;

        case '.' :
          if (sb.length() == 0)
            throw new IllegalArgumentException(
                "Invalid path '" + stringRepresentation + "': zero-length segment at " + i);

          parts.add(sb.toString());
          sb.setLength(0);
          break;

        default :
          sb.append(c);
      }
    }

    // last segment
    if (sb.length() == 0)
      throw new IllegalArgumentException(
          "Invalid path '" + stringRepresentation + "': zero-length segment at " + stringRepresentation.length());

    parts.add(sb.toString());

    return new Specifier(parts.toArray(new String[parts.size()]), 0);
  }

  private final String[] parts;

  private final int offset;

  @VisibleForTesting
  Specifier(final String... parts) {
    this.parts = parts;
    this.offset = 0;
  }

  @VisibleForTesting
  Specifier(final String[] parts, final int offset) {
    this.parts = parts;
    this.offset = offset;
  }

  /**
   * Return whether the specifier has more than the one element (i.e. length() &gt; 1).
   *
   * @return Whether the specifier has more than one element
   */
  public boolean hasMore() {
    return length() > 1;
  }

  /**
   * Return whether the specifier is empty (i.e. length() == 0)
   *
   * @return Whether the specifier has is empty
   */
  public boolean empty() {
    return length() <= 0;
  }

  /**
   * Return the current element, i.e. the element at index 0.
   *
   * @return the element at index 0
   * @throws ArrayIndexOutOfBoundsException if the specifier is empty
   */
  public String head() {
    return element(0);
  }

  /**
   * Return the current element at the given index.
   *
   * @param index the index
   * @return the element at the given index
   * @throws ArrayIndexOutOfBoundsException if the index is outside the length of the specifier
   */
  public String element(final int index) {
    if (index < 0)
      throw new ArrayIndexOutOfBoundsException();

    return parts[this.offset + index];
  }

  /**
   * Return a specifier containing only the elements after the current one. For a specifier of
   * length 1 the empty specifier is returned.
   *
   * @return a new specifier
   * @throws ArrayIndexOutOfBoundsException if this specifier is empty
   */
  public Specifier suffix() {
    if (offset > parts.length - 1)
      throw new ArrayIndexOutOfBoundsException("The specifier has no more tail elements");

    if (offset == parts.length - 1)
      return EMPTY;

    return new Specifier(parts, offset + 1);
  }

  /**
   * Return the suffix specifier by removing the given prefix.
   *
   * @param prefix the prefix
   * @return the suffix specifier
   * @throws IllegalArgumentException if the given prefix isn't actually a prefix of this specifier
   */
  public Specifier suffix(final Specifier prefix) {
    if (!startsWith(prefix))
      throw new IllegalArgumentException(prefix + " is not a prefix of " + this);

    return new Specifier(parts, offset + prefix.length());
  }

  /**
   * The length of specifier, i.e. the number of elements it conststs of.
   *
   * @return the length
   */
  public int length() {
    return parts.length - offset;
  }

  /**
   * Return whether this specifier starts with the given prefix.
   *
   * @param prefix the prefix
   * @return <code>true</code> if it does start with the given prefix
   */
  public boolean startsWith(final Specifier prefix) {
    if (length() < prefix.length())
      return false;

    for (int i = 0; i < prefix.length(); i++) {
      if (!element(i).equals(prefix.element(i)))
        return false;
    }

    return true;
  }

  /**
   * Return whether this identifier starts with the given element. The element is not supposed to
   * have structure - it isn't parsed before matching.
   *
   * @param element the element
   * @return whether the element is matched
   */
  public boolean startsWith(final String element) {
    return head().equals(element);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;

    for (int i = 0; i < length(); i++) {
      result = prime * result + element(i).hashCode();
    }

    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    Specifier other = (Specifier) obj;
    if (length() != other.length())
      return false;

    return startsWith(other);
  }

  @Override
  public String toString() {
    return Arrays.toString(copyOfRange(parts, offset, parts.length));
  }

  /**
   * Reassemble everything from the current element into a string of the form it was parsed from,
   * i.e. by joining the elements with dots and escaping them.
   *
   * @return the reassembled partial specifier string
   */
  public String asString() {
    StringBuilder sb = new StringBuilder();

    for (int i = offset; i < parts.length; i++) {
      if (sb.length() > 0)
        sb.append('.');

      // escaping
      String escaped = parts[i];
      if (escaped.contains("\\"))
        escaped = escaped.replace("\\", "\\\\");
      if (escaped.contains("."))
        escaped = escaped.replace(".", "\\.");

      sb.append(escaped);
    }

    return sb.toString();
  }

  /**
   * Create a new specifier by appending the given elements to this specifier.
   * 
   * @param elements the elements to append
   * @return a new specifier consisting of all elements from this one with the given ones appended
   */
  public Specifier append(final String... elements) {
    String[] concatenated = new String[parts.length - offset + elements.length];
    arraycopy(parts, offset, concatenated, 0, parts.length - offset);
    arraycopy(elements, 0, concatenated, parts.length - offset, elements.length);
    return new Specifier(concatenated, 0);
  }
}
