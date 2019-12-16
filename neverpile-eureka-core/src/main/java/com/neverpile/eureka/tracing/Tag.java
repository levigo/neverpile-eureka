package com.neverpile.eureka.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

/**
 * Parameter annotation used on methods annotated with {@link TraceInvocation} used to indicate that the
 * given method parameter should used as the value of a span tag.
 */
@Target({
    ElementType.PARAMETER, ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface Tag {
  public static class NoopMapper implements Function<Object, Object> {
    @Override
    public Object apply(final Object t) {
      return t; // just a dummy
    }
  }

  /**
   * The name of the tag for a traced parameter.
   * @return The name of the tag.
   */
  String name();

  /**
   * An optional implementation of a {@link Function} used to map from the argument value to the tag
   * value.
   * @return Function to map a non standard value for traceing.
   */
  Class<? extends Function<? extends Object, ? extends Object>> valueAdapter() default NoopMapper.class;
}
