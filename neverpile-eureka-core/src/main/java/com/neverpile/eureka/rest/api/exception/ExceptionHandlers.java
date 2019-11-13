package com.neverpile.eureka.rest.api.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@ControllerAdvice
public class ExceptionHandlers extends ResponseEntityExceptionHandler {
  @Autowired
  private ObjectMapper objectMapper;

  @ExceptionHandler({
      ValidationError.class
  })
  public ResponseEntity<Object> handleConstraintViolation(final ValidationError ex, final HttpHeaders headers,
      final HttpStatus status, final WebRequest request) {
    ArrayNode validationResults = objectMapper.createArrayNode();
    ex.violations.forEach(
        v -> validationResults.addObject().put("facet", v.getFacet().getName()).put("reason", v.getReason()));

    return handleExceptionInternal(ex, validationResults, headers, status, request);
  }
}
