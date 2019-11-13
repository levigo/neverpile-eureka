package com.neverpile.eureka.api.index;

import java.util.Arrays;

public class Field extends Schema {
  public enum Type {
    Text,
    Keyword,
    Integer,
    Number,
    Date,
    Time,
    DateTime,
    Boolean,
    Binary,
    JSON, 
    Object,
  }
  
  private Type type;
  
  private Type alternativeTypes[];
  
  private float boost = 1.0f;

  public Field() {
  }
  
  public Field(final String name, final Type type, final Type... alternativeTypes) {
    super(name);
    this.type = type;
    this.alternativeTypes = alternativeTypes;
  }

  public Type getType() {
    return type;
  }

  public void setType(final Type type) {
    this.type = type;
  }

  public Type[] getAlternativeTypes() {
    return alternativeTypes;
  }

  public void setAlternativeTypes(final Type[] alternativeTypes) {
    this.alternativeTypes = alternativeTypes;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Arrays.hashCode(alternativeTypes);
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    Field other = (Field) obj;
    if (!Arrays.equals(alternativeTypes, other.alternativeTypes))
      return false;
    if (type != other.type)
      return false;
    return true;
  }

  public float getBoost() {
    return boost;
  }

  public Field withBoost(final float boost) {
    this.boost = boost;
    return this;
  }
}
