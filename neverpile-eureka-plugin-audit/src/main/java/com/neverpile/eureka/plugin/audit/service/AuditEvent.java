package com.neverpile.eureka.plugin.audit.service;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import com.neverpile.eureka.model.EncryptableElement;

public class AuditEvent extends EncryptableElement implements Serializable {

  public enum Type {
    CREATE, UPDATE, DELETE;
  }

  private String auditId;
  private Date timestamp;
  private String userID;
  private Type type;
  private String description;
  private String requestPath;
  private String documentId;
  private Byte[] contentHash;

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Date timestamp) {
    this.timestamp = timestamp;
  }

  public String getUserID() {
    return userID;
  }

  public void setUserID(final String userID) {
    this.userID = userID;
  }

  public Type getType() {
    return type;
  }

  public void setType(final Type type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getAuditId() {
    return auditId;
  }

  public void setAuditId(String id) {
    this.auditId = id;
  }

  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(String documentId) {
    this.documentId = documentId;
  }

  public Byte[] getContentHash() {
    return contentHash;
  }

  public void setContentHash(Byte[] contentHash) {
    this.contentHash = contentHash;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), timestamp, userID, type, description);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    return true;
  }

  public String getRequestPath() {
    return requestPath;
  }

  public void setRequestPath(String requestPath) {
    this.requestPath = requestPath;
  }

}
