package com.neverpile.eureka.rest.configuration;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

@Configuration
@ComponentScan
public class JacksonConfiguration {
  @Bean
  Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
    return new Jackson2ObjectMapperBuilderCustomizer() {

      @Override
      public void customize(final Jackson2ObjectMapperBuilder b) {
        b.annotationIntrospector(new AnnotationIntrospectorPair(new JacksonAnnotationIntrospector(),
            new JaxbAnnotationIntrospector(TypeFactory.defaultInstance())));
        b.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        b.dateFormat(new StdDateFormat());
      }
    };
  }
}
