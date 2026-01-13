package com.neverpile.eureka.rest.configuration;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

@Configuration
@ComponentScan
@Import(FacetedDocumentDtoModule.class)
public class JacksonConfiguration {

  @Bean
  JsonMapperBuilderCustomizer jacksonCustomizer() {
    return builder -> builder.addModule(new JakartaXmlBindAnnotationModule());
  }
}
