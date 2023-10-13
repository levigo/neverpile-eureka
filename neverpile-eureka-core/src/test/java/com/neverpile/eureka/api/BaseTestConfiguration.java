package com.neverpile.eureka.api;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.neverpile.eureka.impl.tx.lock.LocalLockFactory;
import com.neverpile.eureka.rest.api.document.DocumentResource;
import com.neverpile.eureka.rest.api.document.content.ContentElementFacet;
import com.neverpile.eureka.rest.api.document.content.ContentElementResource;
import com.neverpile.eureka.rest.api.document.core.CreationDateFacet;
import com.neverpile.eureka.rest.api.document.core.IdFacet;
import com.neverpile.eureka.rest.api.document.core.ModificationDateFacet;
import com.neverpile.eureka.rest.configuration.FacetedDocumentDtoModule;
import com.neverpile.eureka.rest.configuration.JacksonConfiguration;
import com.neverpile.eureka.rest.configuration.ModelMapperConfiguration;
import com.neverpile.eureka.rest.mocks.MockObjectStoreService;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableHypermediaSupport(type = HypermediaType.HAL)
@Import({
    JacksonConfiguration.class, FacetedDocumentDtoModule.class, DocumentResource.class, ModelMapperConfiguration.class,
    ContentElementResource.class, ContentElementFacet.class, IdFacet.class,
    CreationDateFacet.class, ModificationDateFacet.class
})
@EnableTransactionManagement
public class BaseTestConfiguration {
  @EnableWebSecurity
  @TestConfiguration
  @Order(SecurityProperties.BASIC_AUTH_ORDER)
  public static class SecurityConfig {
    @Bean
    SecurityFilterChain web(HttpSecurity http) throws Exception {
//      TODO: set requestMatchers back to "/api/**"
      http //
          .csrf(AbstractHttpConfigurer::disable)
          .httpBasic(Customizer.withDefaults())
          .authorizeHttpRequests(e -> e.requestMatchers("**").hasRole("USER"));
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


//  TODO: replace this with a real transaction manager
  @Primary
  @Bean
  public PlatformTransactionManager transactionManager() {
    return new PlatformTransactionManager() {
      @Override
      public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        return null;
      }

      @Override
      public void commit(TransactionStatus status) throws TransactionException {

      }

      @Override
      public void rollback(TransactionStatus status) throws TransactionException {

      }
    };
  }
}
