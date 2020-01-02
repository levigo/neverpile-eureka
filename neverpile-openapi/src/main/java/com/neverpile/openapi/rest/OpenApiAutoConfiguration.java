package com.neverpile.openapi.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class OpenApiAutoConfiguration {
  @Bean
  public OpenApiDefinitionResource openApiDefinitionResource() {
    return new OpenApiDefinitionResource();
  }
  
  @Bean
  public SwaggerUIConfiguration swaggerUIConfiguration() {
    return new SwaggerUIConfiguration();
  }
  
  @Bean
  public SwaggerUIConfigurationResource swaggerUIConfigurationResource() {
    return new SwaggerUIConfigurationResource();
  }
}
