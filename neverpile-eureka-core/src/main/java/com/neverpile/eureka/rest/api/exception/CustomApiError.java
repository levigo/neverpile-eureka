package com.neverpile.eureka.rest.api.exception;

import java.util.Date;

import org.springframework.http.HttpStatus;

public class CustomApiError {

  private Date timestamp;
  private HttpStatus status;
  private int code;
  private String message;
  private String error;
  private String path;

  public CustomApiError(HttpStatus status, String message, String error, Date timestamp, String path) {
    this.status = status;
    this.code = status.value();
    this.message = message;
    this.error = error;
    this.timestamp = timestamp;
    this.path = path;
  }

  public HttpStatus getStatus() {
    return this.status;
  }

  public String getMessage() {
    return message;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public int getCode() {
    return code;
  }

  public String getError() {
    return error;
  }

  public String getPath() {
    return path;
  }
}
