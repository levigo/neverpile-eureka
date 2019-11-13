package com.neverpile.eureka.rest.configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

/**
 * Workaround for https://github.com/spring-projects/spring-hateoas/issues/705: 
 */
@Component
public class SpringHateoasIssue705WorkAround implements BeanPostProcessor {
  @Autowired
  FacetedDocumentDtoModule m1;
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.
   * lang.Object, java.lang.String)
   */
  @Override
  public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
    if (!"_halObjectMapper".equals(beanName)) {
      return bean;
    }

    ObjectMapper mapper = (ObjectMapper) bean;
    mapper.registerModule(m1);
    mapper.setAnnotationIntrospector(new AnnotationIntrospectorPair(new JacksonAnnotationIntrospector(),
        new JaxbAnnotationIntrospector(TypeFactory.defaultInstance())));
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setDateFormat(new StdDateFormat());

    return mapper;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java
   * .lang.Object, java.lang.String)
   */
  @Override
  public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
    return bean;
  }
}