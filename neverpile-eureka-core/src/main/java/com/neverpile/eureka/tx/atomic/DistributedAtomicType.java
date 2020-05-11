package com.neverpile.eureka.tx.atomic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * This Type is to differentiate between Types of {@link DistributedAtomicReference}s. With this Annotation it is
 * possible to have multiple types of {@link DistributedAtomicReference}s which are distributed and synchronized
 * independently. All {@link DistributedAtomicReference}s must have this annotation.
 */
@Autowired
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedAtomicType {
  String value();

}
