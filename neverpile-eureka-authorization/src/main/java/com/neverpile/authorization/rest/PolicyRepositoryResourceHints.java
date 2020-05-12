package com.neverpile.authorization.rest;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.neverpile.common.authorization.api.HintRegistrations;
import com.neverpile.common.authorization.policy.ResourceHints;

@Component
@ResourceHints
public class PolicyRepositoryResourceHints implements HintRegistrations {
  @Override
  public List<Hint> getHints() {
    return Arrays.asList( //
        new Hint(PolicyRepositoryResource.POLICY_RESOURCE_SPECIFIER, "authorization.policy") //
    );
  }
}