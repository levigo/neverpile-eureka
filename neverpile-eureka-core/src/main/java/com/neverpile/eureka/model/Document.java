package com.neverpile.eureka.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "documentId", "versionTimestamp", "dateCreated", "dateModified", "sidecar", "contentElements" })
public class Document {
  private String documentId;

  @JsonInclude(Include.NON_NULL)
  private Instant versionTimestamp;
  
  private Date dateCreated;

  private Date dateModified;

  private List<ContentElement> contentElements = new ArrayList<ContentElement>();

  public Document() {
  }

  public Document(final String documentId) {
    this.documentId = documentId;
  }

  @XmlAttribute
  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(final String id) {
    this.documentId = id;
  }

  public Instant getVersionTimestamp() {
    return versionTimestamp;
  }

  public void setVersionTimestamp(final Instant versionTimestamp) {
    this.versionTimestamp = versionTimestamp;
  }

  public Date getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(final Date creationDate) {
    this.dateCreated = creationDate;
  }

  public List<ContentElement> getContentElements() {
    return contentElements;
  }

  public void setContentElements(final List<ContentElement> contentElements) {
    this.contentElements = contentElements;
  }

  public void addContentElements(final List<ContentElement> contentElementList) {
    for (ContentElement newContent : contentElementList) {
        this.contentElements.add(newContent);
    }
  }

  public Date getDateModified() {
    return dateModified;
  }

  public void setDateModified(final Date dateModified) {
    this.dateModified = dateModified;
  }
}
