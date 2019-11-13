package com.neverpile.authorization.policy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

import com.neverpile.authorization.api.HintRegistrations;

/**
 * A qualifier for {@link HintRegistrations} pertaining to {@link AccessRule#getResources()}.
 */
@Target({ElementType.FIELD,
  ElementType.METHOD,
  ElementType.TYPE,
  ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface ResourceHints {

}
