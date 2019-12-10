package com.neverpile.eureka.rest.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Workaround for https://github.com/spring-projects/spring-hateoas/issues/705: 
 */
//@Component
public class SpringHateoasIssue705WorkAround implements BeanPostProcessor {
  @Autowired(required = false)
  FacetedDocumentDtoModule m1;
}