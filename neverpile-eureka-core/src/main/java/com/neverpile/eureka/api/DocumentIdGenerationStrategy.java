package com.neverpile.eureka.api;

public interface DocumentIdGenerationStrategy {

  String createDocumentId();

  boolean validateDocumentId(String id);

}
