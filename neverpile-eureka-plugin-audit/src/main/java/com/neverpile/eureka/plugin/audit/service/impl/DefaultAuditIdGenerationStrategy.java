package com.neverpile.eureka.plugin.audit.service.impl;

import java.util.Date;

import com.neverpile.eureka.plugin.audit.service.TimeBasedAuditIdGenerationStrategy;

public class DefaultAuditIdGenerationStrategy implements TimeBasedAuditIdGenerationStrategy {

  private String delimiter = "$";

  @Override
  public String createAuditId(Date timestamp, String documentId) {
    return Long.toString(timestamp.getTime()) + delimiter + documentId;
  }

  @Override
  public String createAuditId(String documentId) {
    return createAuditId(new Date(), documentId);
  }

  @Override
  public boolean validateDocumentId(String id) {
    return id.matches("^\\d+\\" + delimiter + ".+");
  }

  @Override
  public String getDocumentId(String id) {
    return id.split(delimiter)[1];
  }

  @Override
  public String getBlockId(String id) {
    return Long.toString((Long.parseLong(id.split("\\" + delimiter)[0]) / 1000000L) * 1000000L);
  }
}
