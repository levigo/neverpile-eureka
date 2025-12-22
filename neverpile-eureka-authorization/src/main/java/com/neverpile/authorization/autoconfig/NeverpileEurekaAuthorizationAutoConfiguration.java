package com.neverpile.authorization.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.neverpile.authorization.rest.PolicyRepositoryResource;
import com.neverpile.common.authorization.api.Action;
import com.neverpile.common.authorization.api.AuthorizationService;
import com.neverpile.common.authorization.api.CoreActionHints;
import com.neverpile.common.authorization.basic.AllowAllAuthorizationService;
import com.neverpile.common.authorization.policy.MutablePolicyRepository;
import com.neverpile.common.authorization.policy.impl.PolicyBasedAuthorizationService;
import com.neverpile.common.condition.CoreConditionRegistry;
import com.neverpile.eureka.autoconfig.NeverpileEurekaAutoConfiguration;

/**
 * Spring-Boot Auto-configuration for the neverpile eureka authorization plugin.
 */
@AutoConfiguration
@ConditionalOnClass(Action.class)
@AutoConfigureBefore(NeverpileEurekaAutoConfiguration.class)
@AutoConfigureAfter(AuthorizationServiceAutoConfiguration.class)
@Import({CoreConditionRegistry.class, CoreActionHints.class})
public class NeverpileEurekaAuthorizationAutoConfiguration {

  /**
   * Provide an implementation of {@link AuthorizationService} which just allows all accesses. Back
   * off if any other implementation is present.
   *
   * @return an AuthorizationService implementation
   */
  @Bean
  @ConditionalOnMissingBean
  AuthorizationService allowAllAuthorizationService() {
    return new AllowAllAuthorizationService();
  }


  @Bean
  @ConditionalOnBean({MutablePolicyRepository.class,PolicyBasedAuthorizationService.class})
  public PolicyRepositoryResource policyRepositoryResource() {
    return new PolicyRepositoryResource();
  }
}
