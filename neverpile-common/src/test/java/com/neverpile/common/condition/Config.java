package com.neverpile.common.condition;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.neverpile.common.condition.config.ConditionModule;

@SpringBootConfiguration
@Import({
    ConditionModule.class
})
public class Config {
  @Bean
  Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
    return b -> {
      b.annotationIntrospector(new JacksonAnnotationIntrospector());
      b.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      b.dateFormat(new StdDateFormat());
    };
  }
}
