package com.neverpile.authorization.service.impl;

import static com.neverpile.authorization.service.impl.SimpleMutablePolicyRepository.EXPIRED_POLICY_REPO_PREFIX;
import static com.neverpile.authorization.service.impl.SimpleMutablePolicyRepository.OBJECT_NAME_FORMATTER;
import static com.neverpile.authorization.service.impl.SimpleMutablePolicyRepository.POLICY_REPO_PREFIX;
import static com.neverpile.eureka.api.ObjectStoreService.NEW_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.authorization.policy.AccessPolicy;
import com.neverpile.authorization.policy.Effect;
import com.neverpile.authorization.service.impl.SimpleMutablePolicyRepository.CacheEntry;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SimpleMutablePolicyRepositoryTest {
  private final class SimpleStoreObject implements ObjectStoreService.StoreObject {
    private final ObjectName name;
    private final String version;
    private final Supplier<InputStream> contentSupplier;
    private byte[] content;

    public SimpleStoreObject(final ObjectName name, final String version, final byte content[]) {
      this(name, version, () -> new ByteArrayInputStream(content));
      this.content = content;
    }

    public SimpleStoreObject(final ObjectName name, final String version, final Supplier<InputStream> contentSupplier) {
      this.name = name;
      this.version = version;
      this.contentSupplier = contentSupplier;
    }

    @Override
    public String getVersion() {
      return version;
    }

    @Override
    public ObjectName getObjectName() {
      return name;
    }

    @Override
    public InputStream getInputStream() {
      return contentSupplier.get();
    }

    @Override
    public String toString() {
      return name.toString() + " -> " + new String(content);
    }
  }

  @Configuration
  @EnableAutoConfiguration
  public static class ServiceConfig {
    @Bean
    SimpleMutablePolicyRepository documentService() {
      return new SimpleMutablePolicyRepository();
    }
  }

  @MockBean
  ObjectStoreService mockObjectStore;

  @MockBean
  CacheManager cacheManager;

  @MockBean
  Cache cache;

  @Autowired
  SimpleMutablePolicyRepository policyRepository;

  @Autowired
  ObjectMapper objectMapper;

  String policyPattern = "{\"validFrom\" : \"2018-01-01\",\"description\": \"%s\",\"default_effect\": \"DENY\",\"rules\": []}";

  private Instant now;

  private ObjectName oneMinuteAgoName;

  private ObjectName inOneHourName;

  private ObjectName threeHoursAgoName;

  private ObjectName oneHourAgoName;

  private Instant oneMinuteAgo;

  private Instant inOneHour;

  private Instant threeHoursAgo;

  private Instant oneHourAgo;

  @Test
  public void testThat_getCurrentWithNoPoliciesReturnsDefaultPolicy() throws Exception {
    // we don't want the default mockery initialized by initMocks()
    Mockito.reset(mockObjectStore);

    AccessPolicy currentPolicy = policyRepository.getCurrentPolicy();

    assertThat(currentPolicy.getDefaultEffect()).isEqualTo(Effect.DENY);
    assertThat(currentPolicy.getDescription()).contains("default");
    assertThat(currentPolicy.getValidFrom()).isBefore(new Date());
    assertThat(currentPolicy.getRules()).hasSize(1);
  }

  @Test
  public void testThat_getCurrentPolicyReturnsActivePolicy() throws Exception {
    AccessPolicy currentPolicy = policyRepository.getCurrentPolicy();

    assertThat(currentPolicy.getDescription()).contains("current");
  }

  @Test
  public void testThat_getCurrentPolicyArchivesExpiredPolicies() throws Exception {
    // we don't want the default mockery initialized by initMocks()
    Mockito.reset(mockObjectStore);

    // update names to not use the expired prefix
    threeHoursAgoName = POLICY_REPO_PREFIX.append(OBJECT_NAME_FORMATTER.format(threeHoursAgo));
    oneHourAgoName = POLICY_REPO_PREFIX.append(OBJECT_NAME_FORMATTER.format(oneHourAgo));

    // assume un-archived policies present
    given(mockObjectStore.list(POLICY_REPO_PREFIX)).willAnswer(i -> {
      return Stream.of( //
          new SimpleStoreObject(threeHoursAgoName, "1", String.format(policyPattern, "older").getBytes(UTF_8)), //
          new SimpleStoreObject(oneHourAgoName, "1", String.format(policyPattern, "old").getBytes(UTF_8)), //
          new SimpleStoreObject(oneMinuteAgoName, "1", String.format(policyPattern, "current").getBytes(UTF_8)), //
          new SimpleStoreObject(inOneHourName, "1", String.format(policyPattern, "upcoming").getBytes(UTF_8)));
    });

    policyRepository.getCurrentPolicy();

    System.out.println(threeHoursAgoName);
    System.out.println(oneHourAgoName);

    verify(mockObjectStore).put(eq(EXPIRED_POLICY_REPO_PREFIX.append(threeHoursAgoName.tail())), eq(NEW_VERSION),
        any());
    verify(mockObjectStore).delete(eq(threeHoursAgoName));
    verify(mockObjectStore).put(eq(EXPIRED_POLICY_REPO_PREFIX.append(oneHourAgoName.tail())), eq(NEW_VERSION), any());
    verify(mockObjectStore).delete(eq(oneHourAgoName));
  }

  @Test
  public void testThat_queryRepositoryReturnsOldCurrentAndUpcomingPolicies() throws Exception {
    assertThat( //
        policyRepository.queryRepository( //
            Date.from(now().minus(1, DAYS)), Date.from(now().plus(1, DAYS)), //
            Integer.MAX_VALUE //
        ).stream().map(AccessPolicy::getDescription) //
    ).containsExactly("older", "old", "past", "current", "upcoming", "later");
  }

  @Test
  public void testThat_queryRepositoryHonorsRangeLowerBound() throws Exception {
    assertThat( //
        policyRepository.queryRepository( //
            Date.from(now().minus(2, HOURS)), Date.from(now().plus(1, DAYS)), //
            Integer.MAX_VALUE //
        ).stream().map(AccessPolicy::getDescription) //
    ).containsExactly("old", "past", "current", "upcoming", "later");
  }

  @Test
  public void testThat_queryRepositoryHonorsRangeUpperBound() throws Exception {
    assertThat( //
        policyRepository.queryRepository( //
            Date.from(now().minus(1, DAYS)), Date.from(now()), //
            Integer.MAX_VALUE //
        ).stream().map(AccessPolicy::getDescription) //
    ).containsExactly("older", "old", "past", "current");
  }

  @Test
  public void testThat_queryUpcomingReturnsOnlyUpcoming() throws Exception {
    assertThat( //
        policyRepository.queryUpcoming(Integer.MAX_VALUE //
        ).stream().map(AccessPolicy::getDescription) //
    ).containsExactly("upcoming", "later");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_saveIsDeniedForCurrentPolicy() throws Exception {
    policyRepository.save(new AccessPolicy() //
        .withDefaultEffect(Effect.DENY) //
        .withValidFrom(Date.from(oneMinuteAgo)) // "current"
    );
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_saveIsDeniedForOldPolicies() throws Exception {
    policyRepository.save(new AccessPolicy() //
        .withDefaultEffect(Effect.DENY) //
        .withValidFrom(Date.from(oneHourAgo)) // "old"
    );
  }

  @Test
  public void testThat_saveAddsNewPolicy() throws Exception {
    Instant inOneMinute = now.plus(1, MINUTES);

    policyRepository.save(new AccessPolicy() //
        .withDefaultEffect(Effect.DENY) //
        .withDescription("new") //
        .withValidFrom(Date.from(inOneMinute)));

    ArgumentCaptor<InputStream> c = ArgumentCaptor.forClass(InputStream.class);
    verify(mockObjectStore).put( //
        eq(POLICY_REPO_PREFIX.append(OBJECT_NAME_FORMATTER.format(inOneMinute))), //
        eq(ObjectStoreService.NEW_VERSION), //
        c.capture());

    assertThat(objectMapper.readValue(c.getValue(), AccessPolicy.class).getDescription()).contains("new");
  }

  @Test
  public void testThat_saveUpdatesPolicy() throws Exception {
    policyRepository.save(new AccessPolicy() //
        .withDefaultEffect(Effect.DENY) //
        .withDescription("new") //
        .withValidFrom(Date.from(inOneHour)));

    ArgumentCaptor<InputStream> c = ArgumentCaptor.forClass(InputStream.class);
    verify(mockObjectStore).put(eq(inOneHourName), eq("1"), c.capture());

    assertThat(objectMapper.readValue(c.getValue(), AccessPolicy.class).getDescription()).contains("new");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThat_deletePolicyDeniesDeleteOfOldPolicy() throws Exception {
    policyRepository.delete(Date.from(oneHourAgo));
  }

  @Test
  public void testThat_deletePolicyDeletesFuturePolicies() throws Exception {
    policyRepository.delete(Date.from(inOneHour));

    verify(mockObjectStore).delete(eq(inOneHourName));
  }

  @Test
  public void testThat_getCurrentPolicy_populatesCache() throws Exception {
    ArgumentCaptor<CacheEntry> valueCaptor = ArgumentCaptor.forClass(CacheEntry.class);
    reset(cacheManager);
    given(cacheManager.getCache(any())).will((n) -> cache);
    given(cache.get(SimpleMutablePolicyRepository.CURRENT_AUTORIZATION_POLICY_KEY)).willReturn(null);
    doNothing().when(cache).put(eq(SimpleMutablePolicyRepository.CURRENT_AUTORIZATION_POLICY_KEY),
        valueCaptor.capture());

    // no cached value
    policyRepository.getCurrentPolicy();

    verify(cacheManager, atLeastOnce()).getCache(SimpleMutablePolicyRepository.CACHE_NAME);
    verify(cache).get(SimpleMutablePolicyRepository.CURRENT_AUTORIZATION_POLICY_KEY);

    // verify cache entry
    assertThat(valueCaptor.getValue().policy.getDescription()).isEqualTo("current");
    // must use fuzzy match as marshaling/unmarshaling may produce sub-second differences  
    assertThat(valueCaptor.getValue().validUntil).isCloseTo(inOneHour, within(1, ChronoUnit.SECONDS));
  }

  @Test
  public void testThat_getCurrentPolicy_populatesCacheNoUpcomingPolicy() throws Exception {
    ArgumentCaptor<CacheEntry> valueCaptor = ArgumentCaptor.forClass(CacheEntry.class);
    reset(cacheManager);
    given(cacheManager.getCache(any())).will((n) -> cache);
    given(cache.get(SimpleMutablePolicyRepository.CURRENT_AUTORIZATION_POLICY_KEY)).willReturn(null);
    doNothing().when(cache).put(eq(SimpleMutablePolicyRepository.CURRENT_AUTORIZATION_POLICY_KEY),
        valueCaptor.capture());

    // in contrast to above, there is no upcoming policy
    given(mockObjectStore.list(POLICY_REPO_PREFIX)).willAnswer(i -> Stream.of(
        new SimpleStoreObject(oneMinuteAgoName, "1", String.format(policyPattern, "current").getBytes(UTF_8))));

    // no cached value
    policyRepository.getCurrentPolicy();

    verify(cacheManager, atLeastOnce()).getCache(SimpleMutablePolicyRepository.CACHE_NAME);
    verify(cache).get(SimpleMutablePolicyRepository.CURRENT_AUTORIZATION_POLICY_KEY);

    // verify cache entry - should use default entry validity of
    // SimpleMutablePolicyRepository.MAX_CURRENT_POLICY_AGE seconds
    assertThat(valueCaptor.getValue().validUntil).isCloseTo(
        Instant.now().plusSeconds(SimpleMutablePolicyRepository.MAX_CURRENT_POLICY_AGE), within(5, ChronoUnit.SECONDS));
  }

  @Test
  public void testThat_getCurrentPolicy_usesCache() throws Exception {
    reset(cacheManager);
    given(cacheManager.getCache(any())).will(n -> cache);
    AccessPolicy thePolicy = new AccessPolicy();
    given(cache.get(SimpleMutablePolicyRepository.CURRENT_AUTORIZATION_POLICY_KEY)).willReturn(
        new SimpleValueWrapper(new CacheEntry(thePolicy, inOneHour)));

    // cache contains valid entry
    assertThat(policyRepository.getCurrentPolicy()).isSameAs(thePolicy);

    verify(cacheManager).getCache(SimpleMutablePolicyRepository.CACHE_NAME);
    verify(cache).get(SimpleMutablePolicyRepository.CURRENT_AUTORIZATION_POLICY_KEY);
    verifyNoMoreInteractions(cache);
    verifyNoMoreInteractions(mockObjectStore);
  }

  @Test
  public void testThat_getCurrentPolicy_ignoresExpiredEntries() throws Exception {
    reset(cacheManager);
    given(cacheManager.getCache(any())).will(n -> cache);
    given(cache.get(SimpleMutablePolicyRepository.CURRENT_AUTORIZATION_POLICY_KEY)).willReturn(
        new SimpleValueWrapper(new CacheEntry(new AccessPolicy(), oneHourAgo)));

    // cache contains expired entry
    assertThat(policyRepository.getCurrentPolicy().getDescription()).isEqualTo("current");

    verify(cache).get(SimpleMutablePolicyRepository.CURRENT_AUTORIZATION_POLICY_KEY);
    verify(mockObjectStore).list(any());
  }

  @Before
  public void initMocks() {
    now = now();

    threeHoursAgo = now.minus(3, HOURS);
    threeHoursAgoName = EXPIRED_POLICY_REPO_PREFIX.append(OBJECT_NAME_FORMATTER.format(threeHoursAgo));

    oneHourAgo = now.minus(1, HOURS);
    oneHourAgoName = EXPIRED_POLICY_REPO_PREFIX.append(OBJECT_NAME_FORMATTER.format(oneHourAgo));

    ObjectName twoMinutesAgoName = POLICY_REPO_PREFIX.append(OBJECT_NAME_FORMATTER.format(now.minus(2, MINUTES)));

    oneMinuteAgo = now.minus(1, MINUTES);
    oneMinuteAgoName = POLICY_REPO_PREFIX.append(OBJECT_NAME_FORMATTER.format(oneMinuteAgo));

    inOneHour = now.plus(1, HOURS);
    inOneHourName = POLICY_REPO_PREFIX.append(OBJECT_NAME_FORMATTER.format(inOneHour));

    ObjectName inTwoHoursName = POLICY_REPO_PREFIX.append(OBJECT_NAME_FORMATTER.format(now.plus(2, HOURS)));

    given(mockObjectStore.list(POLICY_REPO_PREFIX)).willAnswer(i -> {
      return Stream.of( //
          new SimpleStoreObject(twoMinutesAgoName, "1", String.format(policyPattern, "past").getBytes(UTF_8)), //
          new SimpleStoreObject(oneMinuteAgoName, "1", String.format(policyPattern, "current").getBytes(UTF_8)), //
          new SimpleStoreObject(inOneHourName, "1", String.format(policyPattern, "upcoming").getBytes(UTF_8)), //
          new SimpleStoreObject(inTwoHoursName, "1", String.format(policyPattern, "later").getBytes(UTF_8)));
    });

    // must also consider expired ones!
    given(mockObjectStore.list(EXPIRED_POLICY_REPO_PREFIX)).willAnswer(i -> {
      return Stream.of( //
          new SimpleStoreObject(threeHoursAgoName, "1", String.format(policyPattern, "older").getBytes(UTF_8)), //
          new SimpleStoreObject(oneHourAgoName, "1", String.format(policyPattern, "old").getBytes(UTF_8)) //
      );
    });

    given(mockObjectStore.get(inOneHourName)).willAnswer(i -> {
      return new SimpleStoreObject(inOneHourName, "1", String.format(policyPattern, "upcoming").getBytes(UTF_8));
    });

    // caching
    given(cacheManager.getCache(any())).will((n) -> new NoOpCache(n.getArgument(0)));
  }
}
