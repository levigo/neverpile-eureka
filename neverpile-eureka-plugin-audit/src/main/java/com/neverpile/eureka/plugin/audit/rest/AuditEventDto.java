package com.neverpile.eureka.plugin.audit.rest;

import java.util.Date;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.neverpile.eureka.rest.api.document.IDto;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AuditEvent", description = "An audit event associated with a document")
public class AuditEventDto extends RepresentationModel<AuditEventDto> implements IDto {
  @Schema(description = "The type of an audit event")
  public enum Type {
    @Schema(description = "A document created event")
    CREATE, 
    @Schema(description = "A document updated event")
    UPDATE, 
    @Schema(description = "A document deleted event")
    DELETE,
    @Schema(description = "A user-defined event")
    CUSTOM
    ;
  }

  private String auditId;
  private Date timestamp;
  private String userID;
  private Type type;
  private String description;

  @Schema(description = "The time at which the event occurred")
  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Date timestamp) {
    this.timestamp = timestamp;
  }

  @Schema(description = "The ID of the used which triggered the event; may be null if the event wasn't triggered by a user")
  public String getUserID() {
    return userID;
  }

  public void setUserID(final String userID) {
    this.userID = userID;
  }

  @Schema(description = "The type of event")
  public Type getType() {
    return type;
  }

  public void setType(final Type type) {
    this.type = type;
  }

  @Schema(description = "A textual desription of the event")
  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  // FIXME: do we really need this?
  @Schema(description = "The ID of this event")
  public String getAuditId() {
    return auditId;
  }

  public void setAuditId(final String id) {
    this.auditId = id;
  }

}
