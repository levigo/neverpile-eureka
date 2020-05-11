package com.neverpile.eureka.tasks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;

import com.neverpile.eureka.tx.atomic.DistributedAtomicReference;

/**
 * This Type is to differentiate between Types of {@link TaskQueue}s. With this Annotation it is
 * possible to have multiple types of {@link TaskQueue}s which are distributed and synchronized
 * independently. All {@link TaskQueue}s must have this annotation.
 */
@Autowired
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedPersistentQueueType {
  String value();

}
