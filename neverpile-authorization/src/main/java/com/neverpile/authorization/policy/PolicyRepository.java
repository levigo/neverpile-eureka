package com.neverpile.authorization.policy;

/**
 * A policy repository is responsible for the management of a set of {@link AccessPolicy}s. In its
 * simplest form, it knows how to return the currently valid/applicable access policy.
 */
public interface PolicyRepository {
  /**
   * Return the current policy. Must always return a policy - if everything fails, return a policy
   * that denies all requests.
   * 
   * @return the currently applicable policy
   */
  AccessPolicy getCurrentPolicy();
}
