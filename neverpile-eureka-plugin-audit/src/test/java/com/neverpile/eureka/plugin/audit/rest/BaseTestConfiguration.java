package com.neverpile.eureka.plugin.audit.rest;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.neverpile.common.authorization.api.Action;
import com.neverpile.common.authorization.api.AuthorizationService;
import com.neverpile.common.authorization.basic.AllowAllAuthorizationService;
import com.neverpile.eureka.api.DocumentAuthorizationService;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.plugin.audit.verification.VerificationService;
import com.neverpile.eureka.rest.api.document.DocumentResource;
import com.neverpile.eureka.rest.configuration.FacetedDocumentDtoModule;
import com.neverpile.eureka.rest.configuration.JacksonConfiguration;
import com.neverpile.eureka.rest.configuration.ModelMapperConfiguration;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableHypermediaSupport(type = HypermediaType.HAL)
@Import({JacksonConfiguration.class, FacetedDocumentDtoModule.class, DocumentResource.class,
    ModelMapperConfiguration.class
})
@EnableTransactionManagement
public class BaseTestConfiguration {
  @EnableWebSecurity
  @TestConfiguration
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public static class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      http //
          .csrf().disable() //
          .httpBasic().and() //
          .authorizeRequests() //
          .requestMatchers("/api/**").hasRole("USER");
      return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
      UserDetails user = User.withDefaultPasswordEncoder() //
          .username("user") //
          .password("password") //
          .roles("USER") //
          .build();
      return new InMemoryUserDetailsManager(user);
    }
  }

  @Bean
  MockObjectStoreService objectStoreService() {
    return new MockObjectStoreService();
  }

  @Bean
  AuthorizationService authorizationService() {
    return new AllowAllAuthorizationService();
  }

  @Bean
  public DocumentAuthorizationService documentAuthorizationService() {
    return new DocumentAuthorizationService() {
      @Override
      public boolean authorizeSubResourceAction(final Document document, final Action action,
          final String... subResourcePath) {
        return true;
      }
    };
  }

  @MockBean
  VerificationService mockVerificationService;
}
