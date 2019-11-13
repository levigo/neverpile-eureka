package com.neverpile.eureka.rest.api.exception;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.http.HttpStatus;

import com.neverpile.eureka.rest.api.document.DocumentFacet.ConstraintViolation;

public class ValidationError extends ApiException {
  private static final long serialVersionUID = 1L;

  public final Collection<ConstraintViolation> violations;

  public ValidationError(final Collection<ConstraintViolation> violations) {
    super(HttpStatus.NOT_ACCEPTABLE.value(),
        "Contstraint violation(s) detected: \n" + Arrays.toString(violations.toArray()));

    this.violations = violations;
  }
}
