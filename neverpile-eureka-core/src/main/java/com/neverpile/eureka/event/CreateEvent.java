package com.neverpile.eureka.event;

import com.neverpile.eureka.model.Document;

public class CreateEvent extends Event {

  Document document;
  
  public CreateEvent(Document doc) {
    super.documentId = doc.getDocumentId();
    this.document = doc;
  }
  
  public Document getDocument() {
    return document;
  }
  
}
