package com.neverpile.authorization.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.neverpile.authorization.api.AuthorizationService;
import com.neverpile.authorization.policy.PolicyRepository;
import com.neverpile.authorization.policy.impl.PolicyBasedAuthorizationService;
import com.neverpile.common.condition.config.ConditionModule;

@AutoConfigureAfter(PolicyRepositoryAutoConfiguration.class)
@ConditionalOnBean(value = PolicyRepository.class)
@ConditionalOnMissingBean(value = AuthorizationService.class)
@Import({ConditionModule.class})
public class AuthorizationServiceAutoConfiguration {
  /**
   * Provide an implementation of {@link AuthorizationService} which uses a {@link PolicyRepository}
   * to retrieve policies from. Back off if any other implementation is present.
   *
   * @return an AuthorizationService implementation
   */
  @Bean
  public PolicyBasedAuthorizationService policyBasedAuthorizationService() {
    return new PolicyBasedAuthorizationService();
  }
}