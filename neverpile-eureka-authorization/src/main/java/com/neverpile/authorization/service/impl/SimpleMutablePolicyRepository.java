package com.neverpile.authorization.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.authorization.api.CoreActions;
import com.neverpile.authorization.policy.AccessPolicy;
import com.neverpile.authorization.policy.AccessRule;
import com.neverpile.authorization.policy.Effect;
import com.neverpile.authorization.policy.MutablePolicyRepository;
import com.neverpile.common.util.VisibleForTesting;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.ObjectStoreService.StoreObject;
import com.neverpile.eureka.model.ObjectName;

public class SimpleMutablePolicyRepository implements MutablePolicyRepository {
  private static final String CURRENT_AUTORIZATION_POLICY_KEY = "'current-autorization-policy'";

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMutablePolicyRepository.class);

  @VisibleForTesting
  static final String OBJECT_NAME_PATTERN = "yyyyMMdd-HHmmss-SSS";

  @VisibleForTesting
  static final DateTimeFormatter OBJECT_NAME_FORMATTER = DateTimeFormatter.ofPattern(OBJECT_NAME_PATTERN) //
      .withZone(ZoneOffset.UTC);

  @VisibleForTesting
  static final ObjectName POLICY_REPO_PREFIX = ObjectName.of("authorization", "policy");

  @VisibleForTesting
  static final ObjectName EXPIRED_POLICY_REPO_PREFIX = ObjectName.of("authorization", "policy", "expired");

  @Autowired
  private ObjectStoreService objectStore;

  @Autowired
  private ObjectMapper mapper;
  
  @Autowired
  private TransactionTemplate housekeepingTransaction;

  @Override
  @Cacheable(cacheNames = "authorization", key = CURRENT_AUTORIZATION_POLICY_KEY) // cache under static key 
  public AccessPolicy getCurrentPolicy() {
    ZonedDateTime now = Instant.now().atZone(ZoneOffset.UTC);

    // fetch current and possibly past policies
    List<StoreObject> policyObjects = objectStore.list(POLICY_REPO_PREFIX) //
        .filter(s -> !s.getObjectName().equals(EXPIRED_POLICY_REPO_PREFIX)) //
        .filter(s -> {
          try {
            return ZonedDateTime.parse(s.getObjectName().tail(), OBJECT_NAME_FORMATTER).isBefore(now);
          } catch (Exception e) {
            LOGGER.error("The object name {} is invalid and cannot be parsed", s.getObjectName(), e);
            return false;
          }
        }) //
        .sorted((a, b) -> a.getObjectName().compareTo(b.getObjectName())) //
        .collect(Collectors.toList());

    // if we didn't find anything we return a fake policy denying everything
    if (policyObjects.isEmpty()) {
      LOGGER.warn("There is not currently valid access policy - falling back to default policy");

      AccessPolicy denyAll = createDefaultPolicy();

      denyAll.setValidFrom(Date.from(now.toInstant()));

      return denyAll;
    }

    if(policyObjects.size() > 1)
      archiveExpiredPolicies(policyObjects);

    // the last one is the currently valid one
    return unmarshalPolicy(policyObjects.get(policyObjects.size() - 1));
  }

  /**
   * Create the default policy to be used if no policy can be found.
   * <p>
   * The default implementation creates a policy which denies everything with one exception: in
   * order to work around the chicken-and-egg problem in new installations, any authenticated user
   * is allowed to create a (the initial) policy.
   * 
   * @return the default policy
   */
  protected AccessPolicy createDefaultPolicy() {
    AccessPolicy defaultPolicy = new AccessPolicy();

    defaultPolicy.setDefaultEffect(Effect.DENY);
    defaultPolicy.setDescription("Ad-hoc default policy");
    defaultPolicy.withRule(new AccessRule() //
        .withName("Allow initial policy creation with any authenticated used") //
        .withSubjects("authenticated") //
        .withResources("authorization.policy") //
        .withActions(CoreActions.GET, CoreActions.QUERY, CoreActions.CREATE) //
        .withEffect(Effect.ALLOW) //
    );

    return defaultPolicy;
  }

  private AccessPolicy unmarshalPolicy(final StoreObject storeObject) {
    try {
      return mapper.readValue(storeObject.getInputStream(), AccessPolicy.class);
    } catch (IOException e) {
      // FIXME: think about better ways to deal with this. Fallback to deny-all policy?
      LOGGER.error("The access policy {} cannot be retrieved/parsed", storeObject.getObjectName(), e);

      throw new RuntimeException("Can't unmarshal current access policy", e);
    }
  }

  private void archiveExpiredPolicies(final List<StoreObject> policyObjects) {
    // move old policies to "expired" subfolder
    if (policyObjects.size() > 1) {
      housekeepingTransaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
      housekeepingTransaction.execute(txStatus -> {
        for (int i = 0; i < policyObjects.size() - 1; i++) {
          StoreObject s = policyObjects.get(i);
  
          // copy...
          try (InputStream is = s.getInputStream()) {
            objectStore.put(EXPIRED_POLICY_REPO_PREFIX.append(s.getObjectName().tail()), ObjectStoreService.NEW_VERSION,
                is);
          } catch (IOException e) {
            // let TX management handle rollback and sort things out
            throw new RuntimeException("Can't archive expired policy", e);
          }
  
          // ...and delete
          objectStore.delete(s.getObjectName());
        }
        return null;
      });
    }
  }

  @Override
  public List<AccessPolicy> queryRepository(final Date from, final Date to, final int limit) {
    ZonedDateTime fromUTC = from.toInstant().atZone(ZoneOffset.UTC);
    ZonedDateTime toUTC = to.toInstant().atZone(ZoneOffset.UTC);

    // fetch current and possibly past policies
    // query both expired and current/upcoming policy prefixes
    return Stream.of(objectStore.list(EXPIRED_POLICY_REPO_PREFIX), objectStore.list(POLICY_REPO_PREFIX)) //
        .flatMap(Function.identity()) //
        .filter(s -> !s.getObjectName().equals(EXPIRED_POLICY_REPO_PREFIX)) //
        // filter by range
        .filter(s -> {
          try {
            ZonedDateTime t = ZonedDateTime.parse(s.getObjectName().tail(), OBJECT_NAME_FORMATTER);
            return t.isAfter(fromUTC) && t.isBefore(toUTC);
          } catch (Exception e) {
            LOGGER.error("The object name {} is invalid and cannot be parsed", s.getObjectName(), e);
            return false;
          }
        })
        // sort, just to be on the safe side
        .sorted((a, b) -> a.getObjectName().tail().compareTo(b.getObjectName().tail()))
        // unmarshal
        .map(s -> {
          try {
            return (AccessPolicy) mapper.readValue(s.getInputStream(), AccessPolicy.class);
          } catch (IOException e) {
            LOGGER.error("The current access policy cannot be retrieved/parsed", e);
            return null;
          }
        })
        // remove error-nulls from previous stage
        .filter(Objects::nonNull) //
        // apply limit
        .limit(limit)
        .collect(Collectors.toList());
  }

  @Override
  public List<AccessPolicy> queryUpcoming(final int limit) {
    return queryRepository(new Date(), new Date(Long.MAX_VALUE), limit);
  }

  @Override
  public AccessPolicy get(final Date startOfValidity) {
    String name = makeObjectNameTail(startOfValidity);

    StoreObject storeObject = objectStore.get(POLICY_REPO_PREFIX.append(name));

    // not found? try historic policies
    if (null == storeObject)
      storeObject = objectStore.get(EXPIRED_POLICY_REPO_PREFIX.append(name));

    if (null == storeObject)
      return null;

    return unmarshalPolicy(storeObject);
  }

  private String makeObjectNameTail(final Date startOfValidity) {
    return OBJECT_NAME_FORMATTER.format(startOfValidity.toInstant());
  }

  @Override
  public void save(final AccessPolicy policy) {
    // an access policy with a null valid-from-date means: make it active immediately
    if (null == policy.getValidFrom()) {
      policy.setValidFrom(new Date());
    } else {
      // validate save request
      if (policy.getValidFrom().toInstant().isBefore(Instant.now()))
        throw new IllegalArgumentException("Creating or updating a policy in the past is not permitted");
    }

    // marshall policy
    try {
      String marshalled = mapper.writeValueAsString(policy);

      ObjectName objectName = POLICY_REPO_PREFIX.append(makeObjectNameTail(policy.getValidFrom()));

      StoreObject existingStoreObject = objectStore.get(objectName);

      LOGGER.info("{} access policy {} ({})", null != existingStoreObject ? "Updating" : "Saving",
          OBJECT_NAME_FORMATTER.format(policy.getValidFrom().toInstant()), objectName);

      objectStore.put(objectName,
          null != existingStoreObject ? existingStoreObject.getVersion() : ObjectStoreService.NEW_VERSION,
          new ByteArrayInputStream(marshalled.getBytes(StandardCharsets.UTF_8)));
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot marshal the provided policy", e);
    }
  }

  @Override
  public boolean delete(final Date startOfValidity) {
    // validate delete request
    if (startOfValidity.toInstant().isBefore(Instant.now()))
      throw new IllegalArgumentException("Creating or updating a policy in the past is not permitted");

    ObjectName objectName = POLICY_REPO_PREFIX.append(makeObjectNameTail(startOfValidity));

    if (null != objectStore.get(objectName)) {
      LOGGER.info("Deleting access policy {} ({})", OBJECT_NAME_FORMATTER.format(startOfValidity.toInstant()),
          objectName);

      objectStore.delete(objectName);
      return true;
    }

    return false;
  }
}
