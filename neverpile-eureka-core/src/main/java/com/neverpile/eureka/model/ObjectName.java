package com.neverpile.eureka.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * An ObjectName represents a hierarchical name consisting of one or more components (elements)
 * which are strings. Object names are immutable.
 */
public class ObjectName implements Serializable, Comparable<ObjectName> {
  @Serial
  private static final long serialVersionUID = 1L;

  private final String[] components;

  /**
   * Create an object name from the given components.
   * 
   * @param components
   */
  private ObjectName(final String... components) {
    this.components = components;
  }

  /**
   * Convert the object name to a string array which contains the name components.
   * 
   * @return a string array
   */
  public String[] to() {
    return Arrays.copyOf(components, components.length);
  }

  /**
   * Return the name components as a stream.
   * 
   * @return a stream of name components
   */
  public Stream<String> stream() {
    return Arrays.stream(components);
  }

  /**
   * Create an object name from the given components.
   * 
   * @param components the name components
   * @return an object name
   */
  public static ObjectName of(final String... components) {
    return new ObjectName(components);
  }

  /**
   * Create a new object name by adding the given components to the end of this object name. 
   * 
   * @param newComponents the components to be concatenated
   * @return an object name
   */
  public ObjectName append(final String newComponents) {
    String[] newNameComponent = Arrays.copyOf(components, components.length + 1);
    newNameComponent[components.length] = newComponents;

    return new ObjectName(newNameComponent);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ObjectName that = (ObjectName) o;

    return Arrays.equals(components, that.components);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(components);
  }

  @Override
  public String toString() {
    return "ObjectName" + Arrays.toString(components);
  }

  /**
   * Return whether this name is a prefix of the given name.
   * 
   * @param that other ObjectName
   * @return <code>true</code>, if this name is a prefix of the given name
   */
  public boolean isPrefixOf(final ObjectName that) {
    if (components.length <= that.components.length)
      for (int i = 0; i < components.length; i++)
        if (!components[i].equals(that.components[i]))
          return false;

    return true;
  }

  @Override
  public int compareTo(final ObjectName o) {
    for (int i = 0; i < components.length && i < o.components.length; i++) {
      String c1 = components[i];
      String c2 = o.components[i];
      int r = c1.compareTo(c2);
      if (r != 0)
        return r;
    }

    if (components.length < o.components.length)
      return -1;
    if (components.length > o.components.length)
      return 1;

    return 0;
  }

  /**
   * Return the name component at the given index
   * 
   * @param index the index
   * @return the name component
   * @throws ArrayIndexOutOfBoundsException if the index is &lt; 0 or &gt;= {@link #length()}
   */
  public String element(final int index) {
    return components[index];
  }

  /**
   * Return the number of name component this object name consists of
   * 
   * @return the number of elements
   */
  public int length() {
    return components.length;
  }

  /**
   * Return the last (trailing) name component.
   * 
   * @return the last name component
   */
  public String tail() {
    return components[components.length - 1];
  }
}
