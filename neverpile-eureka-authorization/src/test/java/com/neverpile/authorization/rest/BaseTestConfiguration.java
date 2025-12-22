package com.neverpile.authorization.rest;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
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
  @Order(SecurityFilterProperties.BASIC_AUTH_ORDER)
  public static class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      http //
          .csrf(AbstractHttpConfigurer::disable)
          .httpBasic(Customizer.withDefaults())
          .authorizeHttpRequests(e -> e.requestMatchers("/api/**").hasRole("USER"));
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
}
