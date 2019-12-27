package com.neverpile.openapi.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class SwaggerUIConfiguration implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(final ResourceHandlerRegistry registry) {
    InputStream swaggerUiWebjarProperties = getClass().getResourceAsStream(
        "/META-INF/maven/org.webjars/swagger-ui/pom.properties");

    if (null == swaggerUiWebjarProperties) {
      // should not happen
      throw new IllegalStateException("Swagger-UI webjar not found");
    }

    try {
      Properties properties = new Properties();
      properties.load(swaggerUiWebjarProperties);
      String version = (String) properties.get("version");

      if (null == version)
        throw new IllegalArgumentException("No version found in maven properties for Swagger-UI webjar");

      registry //
          .addResourceHandler("/swagger-ui/**") //
          .addResourceLocations( //
              "/swagger-ui/", // our own resources go first
              "/webjars/swagger-ui/" + version + "/" // then the ones from the webjar
          );
    } catch (IOException e) {
      throw new IllegalArgumentException("Can't load maven properties for Swagger-UI webjar", e);
    }
  }
}