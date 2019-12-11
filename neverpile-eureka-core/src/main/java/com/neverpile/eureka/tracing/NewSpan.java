package com.neverpile.eureka.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used on methods to indicate that a new opentracing span should be created around the
 * method invocation. Span tags can be assigned from method parameters using {@link SpanTag}
 */
@Target({
    ElementType.METHOD, ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface NewSpan {
  String operationName() default "";
}
