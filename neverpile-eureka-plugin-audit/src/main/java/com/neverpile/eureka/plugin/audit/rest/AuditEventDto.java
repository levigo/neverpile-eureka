package com.neverpile.eureka.plugin.audit.rest;

import java.time.Instant;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.neverpile.eureka.rest.api.document.IDto;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEventDto extends RepresentationModel<AuditEventDto> implements IDto {
  public enum Type {
    CREATE, 
    UPDATE, 
    DELETE,
    CUSTOM
    ;
  }

  private String auditId;
  private Instant timestamp;
  private String userID;
  private Type type;
  private String description;
  private String documentId;

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Instant timestamp) {
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

  public void setAuditId(final String id) {
    this.auditId = id;
  }

  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(final String documentId) {
    this.documentId = documentId;
  }
}
