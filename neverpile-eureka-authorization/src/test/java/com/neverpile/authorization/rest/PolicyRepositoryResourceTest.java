package com.neverpile.authorization.rest;

import static com.neverpile.common.authorization.api.CoreActions.CREATE;
import static com.neverpile.common.authorization.api.CoreActions.DELETE;
import static com.neverpile.common.authorization.api.CoreActions.GET;
import static com.neverpile.common.authorization.api.CoreActions.QUERY;
import static com.neverpile.common.authorization.api.CoreActions.UPDATE;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.neverpile.common.authorization.api.ActionHints;
import com.neverpile.common.authorization.api.CoreActionHints;
import com.neverpile.common.authorization.api.CoreActions;
import com.neverpile.common.authorization.api.HintRegistrations;
import com.neverpile.common.authorization.policy.AccessPolicy;
import com.neverpile.common.authorization.policy.AccessRule;
import com.neverpile.common.authorization.policy.Effect;
import com.neverpile.common.authorization.policy.MutablePolicyRepository;
import com.neverpile.common.authorization.policy.ResourceHints;
import com.neverpile.common.authorization.policy.SubjectHints;
import com.neverpile.common.authorization.policy.impl.PolicyBasedAuthorizationService;
import com.neverpile.common.condition.EqualsCondition;
import com.neverpile.common.condition.ExistsCondition;
import com.neverpile.eureka.rest.configuration.ModelMapperConfiguration;
import com.neverpile.eureka.test.AbstractRestAssuredTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PolicyRepositoryResourceTest extends AbstractRestAssuredTest {
  @TestConfiguration
  @Import({PolicyRepositoryResource.class, ModelMapperConfiguration.class, CoreActionHints.class,
      PolicyBasedAuthorizationService.class
  })
  public static class ServiceConfig {
    @Bean
    @ResourceHints
    public HintRegistrations resourceHints() {
      return new HintRegistrations() {
        @Override
        public List<Hint> getHints() {
          return Arrays.asList( //
              new Hint("foo-resource", "foo1"), //
              new Hint("bar-resource", "bar1") //
          );
        }
      };
    }

    @Bean
    @ActionHints
    public HintRegistrations actionHints() {
      return new HintRegistrations() {
        @Override
        public List<Hint> getHints() {
          return Arrays.asList( //
              new Hint("foo-action", "foo1"), //
              new Hint("bar-action", "bar1") //
          );
        }
      };
    }

    @Bean
    @SubjectHints
    public HintRegistrations subjectHints() {
      return new HintRegistrations() {
        @Override
        public List<Hint> getHints() {
          return Arrays.asList( //
              new Hint("foo-subject", "foo1"), //
              new Hint("bar-subject", "bar1") //
          );
        }
      };
    }
  }

  @MockBean
  MutablePolicyRepository mockPolicyRepository;

  @MockBean
  PolicyBasedAuthorizationService mockAuthService;

  private Instant oneMinuteAgo;

  private Instant inOneHour;

  private Instant threeHoursAgo;

  private Instant oneHourAgo;

  private Instant now;

  DateTimeFormatter formatter =  DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(ZoneId.of("UTC"));

  @Before
  public void initMocks() {
    given(mockAuthService.isAccessAllowed(any(), any(), any())).willReturn(true);

    now = Instant.now();
    oneMinuteAgo = now.minus(1, MINUTES);
    inOneHour = now.plus(1, HOURS);
    threeHoursAgo = now.minus(3, HOURS);
    oneHourAgo = now.minus(1, HOURS);

    // Fix date precision to match used date fromat:
    now = Instant.parse(formatter.format(now));
    oneMinuteAgo = Instant.parse(formatter.format(oneMinuteAgo));
    inOneHour = Instant.parse(formatter.format(inOneHour));
    threeHoursAgo = Instant.parse(formatter.format(threeHoursAgo));
    oneHourAgo = Instant.parse(formatter.format(oneHourAgo));

  }

  @Test
  public void testThat_currentPolicyRetrievalWorks() throws JsonProcessingException {
    given(mockPolicyRepository.getCurrentPolicy()).willReturn( //
        new AccessPolicy() //
            .withDescription("current") //
            .withRule(new AccessRule() //
                .withEffect(Effect.ALLOW) //
                .withName("a sample rule") //
                .withActions(CoreActions.GET) //
                .withSubjects("authenticated") //
                .withResources("some.resource") //
                .withCondition(new ExistsCondition() //
                    .withTarget("some.target"))));

    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
      .when()
        .get("/api/v1/authorization/policy/current")
      .then()
        .log().all()
        .statusCode(200)
        .body("$", hasKey("rules"))
        .body("description", equalTo("current"))
        .body("rules", hasSize(1))
        .body("rules[0].name", equalTo("a sample rule"))
        .body("rules[0].effect", equalTo("ALLOW"))
        .body("rules[0].subjects", hasSize(1))
        .body("rules[0].subjects[0]", equalTo("authenticated"))
        .body("rules[0].resources", hasSize(1))
        .body("rules[0].resources[0]", equalTo("some.resource"))
        .body("rules[0].actions", hasSize(1))
        .body("rules[0].actions[0]", equalTo("core:GET"))
        .body("rules[0].conditions.exists.targets", hasSize(1))
        .body("rules[0].conditions.exists.targets[0]", equalTo("some.target"))
        ;
    // @formatter:on

    verify(mockAuthService).isAccessAllowed(eq("authorization.policy"), eq(singleton(GET)), any());
  }

  @Test
  public void testThat_policyListingWorks() throws JsonProcessingException {
    given(mockPolicyRepository.queryRepository(any(), any(), anyInt())).willReturn( //
        Arrays.asList( //
            new AccessPolicy().withDescription("older").withValidFrom(threeHoursAgo), //
            new AccessPolicy().withDescription("old").withValidFrom(oneHourAgo), //
            new AccessPolicy().withDescription("current").withValidFrom(oneMinuteAgo), //
            new AccessPolicy().withDescription("upcoming").withValidFrom(inOneHour) //
        ));

    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .param("from", formatter.format(threeHoursAgo))
        .param("to", formatter.format(inOneHour))
        .param("limit", 4711)
        .auth().preemptive().basic("user", "password")
        .log().all()
      .when()
        .log().all()
        .get("/api/v1/authorization/policy")
      .then()
        .log().all()
        .statusCode(200)
        .body("$", hasSize(4))
        .body("[0].description", equalTo("older"))
        .body("[3].description", equalTo("upcoming"))
        ;
    // @formatter:on

    verify(mockAuthService).isAccessAllowed(eq("authorization.policy"), eq(singleton(QUERY)), any());

    verify(mockPolicyRepository).queryRepository(eq(threeHoursAgo), eq(inOneHour), eq(4711));
  }

  @Test
  public void testThat_policyCreationViaPostWorks() throws JsonProcessingException {
    AccessRule testAR = new AccessRule().withName("newAR").withEffect(Effect.DENY).withCondition(new EqualsCondition());
    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(new AccessPolicy().withValidFrom(now).withDescription("new").withRule(testAR))
        .auth().preemptive().basic("user", "password")
        .log().all()
      .when()
        .log().all()
        .post("/api/v1/authorization/policy")
      .then()
        .log().all()
        .statusCode(200)
        ;
    // @formatter:on

    verify(mockAuthService).isAccessAllowed(eq("authorization.policy"), eq(singleton(CREATE)), any());

    ArgumentCaptor<AccessPolicy> c = ArgumentCaptor.forClass(AccessPolicy.class);
    verify(mockPolicyRepository).save(c.capture());

    // Condition has ben set.
    assertThat(c.getValue().getRules().size()).isEqualTo(1);
  }

  @Test
  public void testThat_policyCreationViaPutWorks() throws JsonProcessingException {
    AccessRule testAR = new AccessRule().withName("newAR").withEffect(Effect.DENY).withCondition(new EqualsCondition());
    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(new AccessPolicy().withDescription("new").withRule(testAR)) // no valid-from here!
        .auth().preemptive().basic("user", "password")
        .log().all()
      .when()
        .log().all()
        .put("/api/v1/authorization/policy/{date}", formatter.format(now))
      .then()
        .log().all()
        .statusCode(200)
        ;
    // @formatter:on

    verify(mockAuthService).isAccessAllowed(eq("authorization.policy"), eq(singleton(CREATE)), any());

    ArgumentCaptor<AccessPolicy> c = ArgumentCaptor.forClass(AccessPolicy.class);
    verify(mockPolicyRepository).save(c.capture());

    // date must have been corrected/set to the one from the url path
    assertThat(c.getValue().getValidFrom()).isEqualTo(now);
    // Condition has ben set.
    assertThat(c.getValue().getRules().size()).isEqualTo(1);
  }

  @Test
  public void testThat_policyUpdateWorks() throws JsonProcessingException {
    given(mockPolicyRepository.get(now)).willReturn( //
        new AccessPolicy() //
            .withDescription("current") //
            .withValidFrom(now));

    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(new AccessPolicy().withValidFrom(now).withDescription("new"))
        .auth().preemptive().basic("user", "password")
        .log().all()
      .when()
        .log().all()
        .post("/api/v1/authorization/policy")
      .then()
        .log().all()
        .statusCode(200)
        ;
    // @formatter:on

    verify(mockAuthService).isAccessAllowed(eq("authorization.policy"), eq(singleton(UPDATE)), any());

    verify(mockPolicyRepository).save(any());
  }

  @Test
  public void testThat_policyDeletionWorks() throws JsonProcessingException {
    given(mockPolicyRepository.get(now)).willReturn( //
        new AccessPolicy() //
            .withDescription("current") //
            .withValidFrom(now));

    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .body(new AccessPolicy().withValidFrom(now).withDescription("new"))
        .auth().preemptive().basic("user", "password")
        .log().all()
      .when()
        .log().all()
        .delete("/api/v1/authorization/policy/{date}", formatter.format(now))
      .then()
        .log().all()
        .statusCode(200)
        ;
    // @formatter:on

    verify(mockAuthService).isAccessAllowed(eq("authorization.policy"), eq(singleton(DELETE)), any());

    verify(mockPolicyRepository).delete(eq(now));
  }

  @Test
  public void testThat_hintsCanBeRetrieved() throws JsonProcessingException {
    // @formatter:off
    RestAssured
      .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .auth().preemptive().basic("user", "password")
        .log().all()
      .when()
        .log().all()
        .get("/api/v1/authorization/policy/hints")
      .then()
        .log().all()
        .statusCode(200)
        .body("actions.prefix", hasItems("core:GET", "core:CREATE", 
            "core:UPDATE", "core:DELETE", "core:QUERY", "core:VALIDATE",
            "foo-action", "bar-action"))
        .body("actions.find{it.prefix=='core:GET'}.documentationKey", equalTo("CoreActions.GET"))
        .body("actions.find{it.prefix=='foo-action'}.documentationKey", equalTo("foo1"))
        .body("actions.find{it.prefix=='bar-action'}.documentationKey", equalTo("bar1"))
        .body("subjects", hasSize(2))
        .body("subjects[0]", hasEntry("prefix", "foo-subject"))
        .body("resources.find{it.prefix=='authorization.policy'}.documentationKey", equalTo("authorization.policy"))
        .body("resources.find{it.prefix=='foo-resource'}.documentationKey", equalTo("foo1"))
        .body("resources.find{it.prefix=='bar-resource'}.documentationKey", equalTo("bar1"))
        ;
    // @formatter:on
  }
}
