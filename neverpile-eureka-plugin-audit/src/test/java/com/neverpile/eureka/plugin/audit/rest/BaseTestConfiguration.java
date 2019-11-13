package com.neverpile.eureka.plugin.audit.rest;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.neverpile.authorization.api.Action;
import com.neverpile.authorization.api.AuthorizationService;
import com.neverpile.authorization.basic.AllowAllAuthorizationService;
import com.neverpile.eureka.api.DocumentAuthorizationService;
import com.neverpile.eureka.impl.tx.lock.NoOpDistributedLock;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentResource;
import com.neverpile.eureka.rest.configuration.FacetedDocumentDtoModule;
import com.neverpile.eureka.rest.configuration.JacksonConfiguration;
import com.neverpile.eureka.rest.configuration.ModelMapperConfiguration;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.tx.lock.DistributedLock;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableHypermediaSupport(type=HypermediaType.HAL)
@Import({JacksonConfiguration.class, FacetedDocumentDtoModule.class, DocumentResource.class, ModelMapperConfiguration.class})
@EnableTransactionManagement
public class BaseTestConfiguration {
  @EnableWebSecurity
  @TestConfiguration
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public static class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(final HttpSecurity http) throws Exception {
      http //
          .csrf().disable() //
          .httpBasic().and() //
          .authorizeRequests() //
          .antMatchers("/api/**").hasRole("USER");
    }

    @Override
    public void configure(final AuthenticationManagerBuilder auth) throws Exception {
      auth.inMemoryAuthentication() //
          .withUser("user").password("{noop}password").roles("USER");
    }
  }
  
  @Bean
  MockObjectStoreService objectStoreService() {
    return new MockObjectStoreService();
  }
  
  @Bean
  DistributedLock noOpLock() {
    return new NoOpDistributedLock();
  }
  
  @Bean
  AuthorizationService authorizationService() {
    return new AllowAllAuthorizationService();
  }
  
  @Bean
  public DocumentAuthorizationService documentAuthorizationService() {
    return new DocumentAuthorizationService() {
      @Override
      public boolean authorizeSubresourceAction(final Document document, final Action action, final String... subResourcePath) {
        return true;
      }
    };
  }
}
