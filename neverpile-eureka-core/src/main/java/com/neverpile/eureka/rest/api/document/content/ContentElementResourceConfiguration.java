package com.neverpile.eureka.rest.api.document.content;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class ContentElementResourceConfiguration implements WebMvcConfigurer {
  @Override
  public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(new AllRequestPartsMethodArgumentResolver());
  }
  
  /**
   * An {@link HttpMessageConverter} supporting all <code>multipart/*</code> media types.
   * 
   * @return an {@link HttpMessageConverter}
   */
  @Bean
  public MultipartMessageConverter mimeMultipartConverter() {
    return new MultipartMessageConverter();
  }
}