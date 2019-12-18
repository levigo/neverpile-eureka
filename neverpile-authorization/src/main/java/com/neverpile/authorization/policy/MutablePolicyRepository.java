package com.neverpile.authorization.policy;

import java.time.Instant;
import java.util.List;

/**
 * A policy repository that also supports mutations, i.e. adding and removing policies. The
 * repository uses the policies' valid-from date as a primary key.
 */
// FIXME: consider using versioning for mutations
public interface MutablePolicyRepository extends PolicyRepository {
  /**
   * Query the repository for {@link AccessPolicy}s with a start-of-validity date
   * ({@link AccessPolicy#getValidFrom()}) between <code>from</code> and <code>to</code> inclusive.
   * The returned list is ordered by ascending validity date. Return at most <code>limit</code>
   * results.
   * <br>
   * If the result list has been limited, it is guaranteed that the list is complete up to the last
   * returned validity date, so that another query starting from that date (plus 1ms) will return
   * the next chunk of hits.
   * 
   * @param from the start date (inclusive) of the validity date range
   * @param to the end date (inclusive) of the validity date range
   * @param limit the maximum number of policies to return
   * @return a list of access policies matching the range
   */
  List<AccessPolicy> queryRepository(Instant from, Instant to, int limit);

  /**
   * Query only upcoming (future) policies. This excludes the currently applicable policy.
   * 
   * @param limit the maximum number of policies to return
   * @return a list of upcoming access policies
   */
  List<AccessPolicy> queryUpcoming(int limit);

  /**
   * Return the access policy with the given valid-from date.
   * 
   * @param startOfValidity the start-of-validity date
   * @return the policy or <code>null</code> if there is no such policy
   */
  AccessPolicy get(Instant startOfValidity);

  /**
   * Save an access policy with the start-of-validity date in the policy. Serves both as a method
   * for adding new policies as well as for updating existing ones.
   * 
   * <h3>Restrictions/usage notes:</h3>
   * <ul>
   * <li>Only upcoming policies can be updated. The current or former policies are immutable.
   * <li>In order the update the currently active policy, a new one with a start-of-validity a few
   * seconds in the future from "now" must be added.
   * <li>Policies with a start-of-validity date in the past cannot be added, since it might specify
   * access rules that would make operations that have already happened invalid.
   * </ul>
   * 
   * @param policy the policy to add or update
   */
  void save(AccessPolicy policy);

  /**
   * Delete the policy with the given start-of-validity date. Returns <code>false</code> if no
   * matching policy exists.
   * 
   * <h3>Restrictions/usage notes:</h3>
   * <ul>
   * <li>Only upcoming policies can be deleted. The current or former policies are immutable.
   * </ul>
   * 
   * @param startOfValidity the start-of-validity date
   * @return <code>true</code> if the delete succeeded, <code>false</code> otherwise
   */
  boolean delete(Instant startOfValidity);
}
