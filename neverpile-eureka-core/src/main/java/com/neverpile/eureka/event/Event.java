package com.neverpile.eureka.event;

public abstract class Event {
  
  protected String documentId;

  public String getDocumentId() {
    return documentId;
  }
  public void setDoc(String documentId) {
    this.documentId = documentId;
  }
}
