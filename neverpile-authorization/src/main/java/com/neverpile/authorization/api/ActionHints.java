package com.neverpile.authorization.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A qualifier for {@link HintRegistrations} pertaining to {@link Action}s.
 */
@Target({ElementType.FIELD,
  ElementType.METHOD,
  ElementType.TYPE,
  ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface ActionHints {

}
