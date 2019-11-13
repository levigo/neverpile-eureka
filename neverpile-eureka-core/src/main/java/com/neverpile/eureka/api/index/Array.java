package com.neverpile.eureka.api.index;

public class Array extends Schema {
  private Schema elementSchema;

  public Array() {
  }
  
  public Array(final Schema elementSchema) {
    this.elementSchema = elementSchema;
  }

  public Schema getElementSchema() {
    return elementSchema;
  }

  public void setElementSchema(final Schema elementSchema) {
    this.elementSchema = elementSchema;
  }
}
