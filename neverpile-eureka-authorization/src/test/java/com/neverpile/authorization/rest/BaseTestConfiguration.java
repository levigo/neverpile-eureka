package com.neverpile.authorization.rest;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.neverpile.common.condition.config.ConditionModule;
import com.neverpile.eureka.rest.configuration.JacksonConfiguration;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import({JacksonConfiguration.class, ConditionModule.class})
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
}
