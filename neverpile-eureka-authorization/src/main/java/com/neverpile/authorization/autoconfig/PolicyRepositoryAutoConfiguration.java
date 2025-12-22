package com.neverpile.authorization.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

import com.neverpile.authorization.service.impl.SimpleMutablePolicyRepository;
import com.neverpile.common.authorization.policy.MutablePolicyRepository;
import com.neverpile.common.authorization.policy.PolicyRepository;
import com.neverpile.eureka.api.ObjectStoreService;

@AutoConfiguration
@ConditionalOnMissingBean(value = PolicyRepository.class)
@ConditionalOnBean(ObjectStoreService.class)
@EnableCaching
public class PolicyRepositoryAutoConfiguration {
  @Bean
  public MutablePolicyRepository simpleMutablePolicyRepository() {
    return new SimpleMutablePolicyRepository();
  }
}