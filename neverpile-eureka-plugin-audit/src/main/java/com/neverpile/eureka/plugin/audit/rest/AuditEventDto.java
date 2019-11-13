package com.neverpile.eureka.plugin.audit.rest;

import java.util.Date;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.neverpile.eureka.rest.api.document.IDto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "AuditEvent", description = "An audit event associated with a document")
public class AuditEventDto extends ResourceSupport implements IDto {
  @ApiModel(description = "The type of an audit event")
  public enum Type {
    @ApiModelProperty(value = "A document created event")
    CREATE, 
    @ApiModelProperty(value = "A document updated event")
    UPDATE, 
    @ApiModelProperty(value = "A document deleted event")
    DELETE,
    @ApiModelProperty(value = "A user-defined event")
    CUSTOM
    ;
  }

  private String auditId;
  private Date timestamp;
  private String userID;
  private Type type;
  private String description;

  @ApiModelProperty(value = "The time at which the event occurred")
  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Date timestamp) {
    this.timestamp = timestamp;
  }

  @ApiModelProperty(value = "The ID of the used which triggered the event; may be null if the event wasn't triggered by a user")
  public String getUserID() {
    return userID;
  }

  public void setUserID(final String userID) {
    this.userID = userID;
  }

  @ApiModelProperty(value = "The type of event")
  public Type getType() {
    return type;
  }

  public void setType(final Type type) {
    this.type = type;
  }

  @ApiModelProperty(value = "A textual desription of the event")
  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  // FIXME: do we really need this?
  @ApiModelProperty(value = "The ID of this event")
  public String getAuditId() {
    return auditId;
  }

  public void setAuditId(final String id) {
    this.auditId = id;
  }

}
