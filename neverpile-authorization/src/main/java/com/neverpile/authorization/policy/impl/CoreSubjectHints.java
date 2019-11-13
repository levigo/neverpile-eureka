package com.neverpile.authorization.policy.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.neverpile.authorization.api.HintRegistrations;
import com.neverpile.authorization.policy.AccessRule;
import com.neverpile.authorization.policy.SubjectHints;

/**
 * {@link HintRegistrations} for the core subject patterns.
 */
@Component
@SubjectHints
public class CoreSubjectHints implements HintRegistrations {
  @Override
  public List<Hint> getHints() {
    return Arrays.asList( //
        new Hint(AccessRule.ANY, "anything"), //
        new Hint(AccessRule.AUTHENTICATED, "any-authenticated-principal"), //
        new Hint(AccessRule.PRINCIPAL, "principal"), //
        new Hint(AccessRule.ROLE, "role"), //
        new Hint(AccessRule.ANONYMOUS_CALLER, "anonymous") //
    );
  }
}
