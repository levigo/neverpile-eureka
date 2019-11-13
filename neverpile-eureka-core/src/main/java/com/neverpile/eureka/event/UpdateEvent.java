package com.neverpile.eureka.event;

import com.neverpile.eureka.model.Document;

public class UpdateEvent extends Event {
  
  Document document;
  
  public UpdateEvent(Document doc) {
    super.documentId = doc.getDocumentId();
    this.document = doc;
  }
  
  public Document getDocument() {
    return document;
  }
}
