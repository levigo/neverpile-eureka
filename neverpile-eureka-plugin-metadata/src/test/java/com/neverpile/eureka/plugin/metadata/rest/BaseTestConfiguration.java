package com.neverpile.eureka.plugin.metadata.rest;

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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.neverpile.common.authorization.api.AuthorizationService;
import com.neverpile.common.authorization.policy.AccessPolicy;
import com.neverpile.common.authorization.policy.PolicyRepository;
import com.neverpile.common.authorization.policy.impl.PolicyBasedAuthorizationService;
import com.neverpile.eureka.api.DocumentAuthorizationService;
import com.neverpile.eureka.impl.authorization.DefaultDocumentAuthorizationService;
import com.neverpile.eureka.impl.tx.lock.LocalLockFactory;
import com.neverpile.eureka.rest.api.document.DocumentResource;
import com.neverpile.eureka.rest.configuration.FacetedDocumentDtoModule;
import com.neverpile.eureka.rest.configuration.JacksonConfiguration;
import com.neverpile.eureka.rest.configuration.ModelMapperConfiguration;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableHypermediaSupport(type=HypermediaType.HAL)
@Import({FacetedDocumentDtoModule.class, JacksonConfiguration.class, DocumentResource.class, ModelMapperConfiguration.class})
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
          .antMatchers("/api/**").hasRole("USER");
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
  ClusterLockFactory noOpLock() {
    return new LocalLockFactory();
  }
  
  @Bean
  AuthorizationService policyBasedAuthorizationService() {
    return new PolicyBasedAuthorizationService();
  }
  
  @Bean
  public DocumentAuthorizationService defaultDocumentAuthorizationService() {
    return new DefaultDocumentAuthorizationService();
  }
  
  @Bean 
  public PolicyRepository dummyPolicyRepository() {
    return new PolicyRepository() {
      @Override
      public AccessPolicy getCurrentPolicy() {
        return null;
      }
    };
  }
}
