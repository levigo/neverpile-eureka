package com.neverpile.authorization.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.CoreMatchers.containsString;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.assertj.core.api.ListAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.neverpile.authorization.policy.AccessPolicy;
import com.neverpile.authorization.policy.Effect;
import com.neverpile.common.condition.AndCondition;
import com.neverpile.common.condition.Condition;
import com.neverpile.common.condition.EqualsCondition;
import com.neverpile.common.condition.ExistsCondition;
import com.neverpile.common.condition.NotCondition;

@RunWith(SpringRunner.class)
@JsonTest
public class PolicyPersistenceTest {
  @Autowired
  ObjectMapper mapper;

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void testThat_policyCanBeDeserialized() throws Exception {
    AccessPolicy policy = (AccessPolicy) mapper.readValue(getClass().getResourceAsStream("example-policy.json"),
        AccessPolicy.class);

    assertThat(policy.getDefaultEffect()).isEqualTo(Effect.DENY);
    assertThat(policy.getDescription()).contains("some policy");
    assertThat(policy.getValidFrom()).isCloseTo(Instant.ofEpochMilli(0),within(0, ChronoUnit.MILLIS));
    assertThat(policy.getRules()).size().isEqualTo(8);
    assertThat(policy.getRules().get(0).getName()).isEqualTo("Superuser-permissions");
    assertThat(policy.getRules().get(0).getEffect()).isEqualTo(Effect.ALLOW);
    assertThat(policy.getRules().get(0).getSubjects()).containsExactly("role:administrator",
        "principal:johnny-superuser");
    assertThat(policy.getRules().get(0).getResources()).containsExactly("*");
    assertThat(policy.getRules().get(0).getConditions()).isInstanceOf(AndCondition.class);
    assertThat(policy.getRules().get(0).getConditions().getConditions()).hasSize(0);

    assertThat(policy.getRules().get(1).getResources()).containsExactly("neverpile:eureka:document");

    assertThat(policy.getRules().get(3).getConditions()).isNotNull();
    assertThat(policy.getRules().get(3).getConditions().getName()).isBlank();
    assertThat(policy.getRules().get(3).getConditions().getConditions()).size().isEqualTo(1);

    assertThat(policy.getRules().get(4).getConditions().getConditions()).size().isEqualTo(2);
    assertThat(policy.getRules().get(4).getConditions().getConditions()).first() //
        .isInstanceOf(ExistsCondition.class) //
        .hasFieldOrPropertyWithValue("name", "Has metadata of type car-insurance").hasFieldOrPropertyWithValue(
        "targets", Arrays.asList("neverpile:eureka:document.metadata.car-insurance", "something:else"));
    assertThat(policy.getRules().get(4).getConditions().getConditions().get(1)).isInstanceOf(NotCondition.class);

    ListAssert<Condition> notCondition = assertThat(
        ((NotCondition) policy.getRules().get(4).getConditions().getConditions().get(1)).getConditions());
    notCondition.hasSize(2);
    notCondition.element(0).isInstanceOf(EqualsCondition.class);
    notCondition.element(1).isInstanceOf(ExistsCondition.class);
  }


  @Test
  public void testThat_unrecognizedPropertyTypeIsRejected() throws Exception {
    exception.expect(UnrecognizedPropertyException.class);
    exception.expectMessage(containsString("foo"));

    mapper.readValue("{\"rules\": [{\"conditions\": { \"foo\":  []}}]}", AccessPolicy.class);
  }
}
