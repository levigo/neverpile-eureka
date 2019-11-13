package com.neverpile.eureka.event;

import com.neverpile.eureka.model.Document;

public class AggregatedUpdateEvent  extends Event {

  Document document;

  public AggregatedUpdateEvent(Document doc) {
    super.documentId = doc.getDocumentId();
    this.document = doc;
  }

  public Document getDocument() {
    return document;
  }
}
