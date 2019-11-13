package com.neverpile.eureka.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DocumentCollection   {
  private String id;
  
  private String displayName;

  public DocumentCollection() {
  }
  
  public DocumentCollection(final String collectionId, final String displayName) {
    id = collectionId;
    this.displayName = displayName;
  }

  @XmlAttribute
  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }
}
