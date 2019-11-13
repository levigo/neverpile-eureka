package com.neverpile.eureka.api.index;

import java.util.HashSet;
import java.util.Set;

import com.neverpile.common.specifier.Specifier;
import com.neverpile.eureka.api.index.Field.Type;

public class Structure extends Schema {
  private Set<Schema> elements = new HashSet<>();

  private boolean dynamic;

  public Structure() {
  }

  public Structure(final String name, final Set<Schema> elements) {
    super(name);
    this.elements = elements;
  }

  public Set<Schema> getElements() {
    return elements;
  }

  public void setElements(final Set<Schema> elements) {
    this.elements = elements;
  }

  public Structure withField(final String name, final Type type, final Type... alternativeTypes) {
    elements.add(new Field(name, type, alternativeTypes));
    return this;
  }

  public Schema withArray(final String name, final Schema elementStructure) {
    elements.add(new Array(elementStructure));
    return this;
  }

  public Schema withDynamicFields() {
    setDynamic(true);
    return this;
  }

  public void setDynamic(final boolean dynamic) {
    this.dynamic = dynamic;
  }

  public boolean isDynamic() {
    return dynamic;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (dynamic ? 1231 : 1237);
    result = prime * result + ((elements == null) ? 0 : elements.hashCode());
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
    Structure other = (Structure) obj;
    if (dynamic != other.dynamic)
      return false;
    if (elements == null) {
      if (other.elements != null)
        return false;
    } else if (!elements.equals(other.elements))
      return false;
    return true;
  }

  public boolean isDynamicBranch(final Specifier path) {
    if (isDynamic())
      return true;

    if(path.empty())
      return false;
    
    return elements.stream().anyMatch(e -> e.getName().equals(path.head()) //
        && e instanceof Structure //
        && ((Structure) e).isDynamicBranch(path.suffix()));
  }
}
