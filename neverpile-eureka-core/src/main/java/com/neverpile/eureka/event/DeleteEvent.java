package com.neverpile.eureka.event;

public class DeleteEvent extends Event {
  
  public DeleteEvent(String documentId) {
    super.documentId = documentId;
  }
}
