package com.neverpile.eureka.tracing.aspect;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.neverpile.eureka.tracing.Tag;
import com.neverpile.eureka.tracing.TraceInvocation;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;

/**
 * An aspect used to create opentracing spans for calls to methods annotated with {@link TraceInvocation}.
 */
@Aspect
@Configuration
@ConditionalOnBean(Tracer.class)
public class OpentracingAspect {
  private static final Logger LOGGER = LoggerFactory.getLogger(OpentracingAspect.class);
  
  @Autowired
  Tracer tracer;

  private final Map<Class<? extends Function<Object, Object>>, Function<Object, Object>> valueAdapterCache = new ConcurrentHashMap<>();

  @PostConstruct
  public void logActivation() {
    LOGGER.info("Opentracing Tracer found - tracing of methods annotated with @TraceInvocation enabled");
  }
  
  @Around("execution (@com.neverpile.eureka.tracing.TraceInvocation * *.*(..))")
  public Object newSpanAround(final ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Object[] args = joinPoint.getArgs();

    Span span = startSpan(signature);

    resolveParameters(signature, args, span);

    try (Scope scope = tracer.scopeManager().activate(span)) {
      Object result = joinPoint.proceed(args);
      return result;
    } catch (Throwable ex) {
      Tags.ERROR.set(span, true);
      Map<String, Object> m = new HashMap<>();
      m.put(Fields.EVENT, "error");
      m.put(Fields.ERROR_OBJECT, ex);
      m.put(Fields.MESSAGE, ex.getMessage());
      span.log(m);
      throw ex;
    } finally {
      span.finish();
    }
  }

  private void resolveParameters(final MethodSignature signature, final Object[] args, final Span span)
      throws Exception {
    Parameter[] parameters = signature.getMethod().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].getAnnotation(Tag.class) != null) {
        setupTag(parameters[i], args[i], span);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void setupTag(final Parameter parameter, final Object arg, final Span span) throws Exception {
    Tag annotation = parameter.getAnnotation(Tag.class);
    String tagKey = annotation.name();

    Object value = arg;
    if (!annotation.valueAdapter().equals(Tag.NoopMapper.class)) {
      value = valueAdapterCache //
          .computeIfAbsent((Class<? extends Function<Object, Object>>) annotation.valueAdapter(),
              c -> (Function<Object, Object>) BeanUtils.instantiateClass(c)) //
          .apply(value);
    }

    setTag(span, tagKey, value);
  }

  private void setTag(final Span span, final String key, final Object value) {
    if (null == value)
      span.setTag(key, "<NULL>");
    else if (value instanceof Number)
      span.setTag(key, (Number) value);
    else if (value instanceof Boolean)
      span.setTag(key, (Boolean) value);
    else
      span.setTag(key, value.toString());
  }

  private Span startSpan(final MethodSignature signature) {
    Span parentSpan = tracer.scopeManager().activeSpan();
    String operationName = getOperationName(signature);

    return tracer.buildSpan(operationName).asChildOf(parentSpan).start();
  }

  private String getOperationName(final MethodSignature signature) {
    String operationName;
    TraceInvocation newSpanAnnotation = signature.getMethod().getAnnotation(TraceInvocation.class);
    if (StringUtils.isEmpty(newSpanAnnotation.operationName())) {
      operationName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    } else {
      operationName = newSpanAnnotation.operationName();
    }

    return operationName;
  }
}
