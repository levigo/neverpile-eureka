package com.neverpile.common.condition;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.common.specifier.Specifier;

@RunWith(SpringRunner.class)
@JsonTest
public class ConditionEvaluationTest {
  private static final Condition ALWAYS = new Condition() {
    @Override
    public boolean matches(final ConditionContext context) {
      return true;
    }
  };

  private static final Condition NEVER = new Condition() {
    @Override
    public boolean matches(final ConditionContext context) {
      return false;
    }
  };

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @MockBean
  ConditionContext ctx;

  @Test
  public void testThat_equalsConditionEvaluationWorks() throws Exception {
    given(ctx.resolveValue(Specifier.from("foo.bar"))).willReturn("baz");

    EqualsCondition equals = new EqualsCondition();

    // positive match
    equals.addPredicate("foo.bar", asList("baz", "baz1"));
    assertThat(equals.matches(ctx)).isTrue();

    // negative match (non-matching value)
    equals.getPredicates().clear();
    equals.addPredicate("foo.bar", asList("something else"));
    assertThat(equals.matches(ctx)).isFalse();

    // negative match (no such value)
    equals.getPredicates().clear();
    equals.addPredicate("yada.yada", asList("yada!"));
    assertThat(equals.matches(ctx)).isFalse();
  }

  @Test
  public void testThat_greaterThanConditionEvaluationWorks() throws Exception {
    given(ctx.resolveValue(Specifier.from("foo.bar"))).willReturn("3");
    given(ctx.resolveValue(Specifier.from("foo.baz"))).willReturn(3);

    GreaterThanCondition greater = new GreaterThanCondition();

    // positive match
    greater.addPredicate("foo.bar", asList("2"));
    assertThat(greater.matches(ctx)).isTrue();

    // positive match
    greater.getPredicates().clear();
    greater.addPredicate("foo.baz",2);
    assertThat(greater.matches(ctx)).isTrue();

    // negative match (logic)
    greater.getPredicates().clear();
    greater.addPredicate("foo.bar", asList("4"));
    assertThat(greater.matches(ctx)).isFalse();

    // negative match (logic)
    greater.getPredicates().clear();
    greater.addPredicate("foo.baz",3);
    assertThat(greater.matches(ctx)).isFalse();

    // negative match (comparison mismatch)
    greater.getPredicates().clear();
    greater.addPredicate("foo.bar",0);
    assertThat(greater.matches(ctx)).isFalse();

    // negative match (no such value)
    greater.getPredicates().clear();
    greater.addPredicate("yada.yada", asList("yada!"));
    assertThat(greater.matches(ctx)).isFalse();
  }

  @Test
  public void testThat_greaterOrEqualsToConditionEvaluationWorks() throws Exception {
    given(ctx.resolveValue(Specifier.from("foo.bar"))).willReturn("3");
    given(ctx.resolveValue(Specifier.from("foo.baz"))).willReturn(3);

    GreaterOrEqualToCondition greaterOrEquals = new GreaterOrEqualToCondition();

    // positive match
    greaterOrEquals.addPredicate("foo.bar", asList("2"));
    assertThat(greaterOrEquals.matches(ctx)).isTrue();

    // positive match
    greaterOrEquals.getPredicates().clear();
    greaterOrEquals.addPredicate("foo.baz",2);
    assertThat(greaterOrEquals.matches(ctx)).isTrue();

    // negative match (logic)
    greaterOrEquals.getPredicates().clear();
    greaterOrEquals.addPredicate("foo.bar", asList("4"));
    assertThat(greaterOrEquals.matches(ctx)).isFalse();

    // negative match (logic)
    greaterOrEquals.getPredicates().clear();
    greaterOrEquals.addPredicate("foo.baz",3);
    assertThat(greaterOrEquals.matches(ctx)).isTrue();

    // negative match (comparison mismatch)
    greaterOrEquals.getPredicates().clear();
    greaterOrEquals.addPredicate("foo.bar",0);
    assertThat(greaterOrEquals.matches(ctx)).isFalse();

    // negative match (no such value)
    greaterOrEquals.getPredicates().clear();
    greaterOrEquals.addPredicate("yada.yada", asList("yada!"));
    assertThat(greaterOrEquals.matches(ctx)).isFalse();
  }

  @Test
  public void testThat_LessThanConditionEvaluationWorks() throws Exception {
    given(ctx.resolveValue(Specifier.from("foo.bar"))).willReturn("3");
    given(ctx.resolveValue(Specifier.from("foo.baz"))).willReturn(3);

    LessThanCondition less = new LessThanCondition();

    // positive match
    less.addPredicate("foo.bar", asList("4"));
    assertThat(less.matches(ctx)).isTrue();

    // positive match
    less.getPredicates().clear();
    less.addPredicate("foo.baz",4);
    assertThat(less.matches(ctx)).isTrue();

    // negative match (logic)
    less.getPredicates().clear();
    less.addPredicate("foo.bar", asList("2"));
    assertThat(less.matches(ctx)).isFalse();

    // negative match (logic)
    less.getPredicates().clear();
    less.addPredicate("foo.baz",3);
    assertThat(less.matches(ctx)).isFalse();

    // negative match (comparison mismatch)
    less.getPredicates().clear();
    less.addPredicate("foo.bar",0);
    assertThat(less.matches(ctx)).isFalse();

    // negative match (no such value)
    less.getPredicates().clear();
    less.addPredicate("yada.yada", asList("yada!"));
    assertThat(less.matches(ctx)).isFalse();
  }

  @Test
  public void testThat_lessOrEqualToConditionEvaluationWorks() throws Exception {
    given(ctx.resolveValue(Specifier.from("foo.bar"))).willReturn("3");
    given(ctx.resolveValue(Specifier.from("foo.baz"))).willReturn(3);

    LessOrEqualToCondition lessOrEquals = new LessOrEqualToCondition();

    // positive match
    lessOrEquals.addPredicate("foo.bar", asList("4"));
    assertThat(lessOrEquals.matches(ctx)).isTrue();

    // positive match
    lessOrEquals.getPredicates().clear();
    lessOrEquals.addPredicate("foo.baz",4);
    assertThat(lessOrEquals.matches(ctx)).isTrue();

    // negative match (logic)
    lessOrEquals.getPredicates().clear();
    lessOrEquals.addPredicate("foo.bar", asList("2"));
    assertThat(lessOrEquals.matches(ctx)).isFalse();

    // negative match (logic)
    lessOrEquals.getPredicates().clear();
    lessOrEquals.addPredicate("foo.baz",3);
    assertThat(lessOrEquals.matches(ctx)).isTrue();

    // negative match (comparison mismatch)
    lessOrEquals.getPredicates().clear();
    lessOrEquals.addPredicate("foo.bar",0);
    assertThat(lessOrEquals.matches(ctx)).isFalse();

    // negative match (no such value)
    lessOrEquals.getPredicates().clear();
    lessOrEquals.addPredicate("yada.yada", asList("yada!"));
    assertThat(lessOrEquals.matches(ctx)).isFalse();
  }

  @Test
  public void testThat_andConditionEvaluationWorks() throws Exception {
    AndCondition and = new AndCondition();

    // empty list -> matches
    assertThat(and.matches(ctx)).isTrue();

    // one positive match
    and.addCondition(ALWAYS);
    assertThat(and.matches(ctx)).isTrue();

    // two positive matches
    and.addCondition(ALWAYS);
    assertThat(and.matches(ctx)).isTrue();

    // one negative, two positive
    and.addCondition(NEVER);
    assertThat(and.matches(ctx)).isFalse();

    // one negative only
    and.getConditions().clear();
    and.addCondition(NEVER);
    assertThat(and.matches(ctx)).isFalse();
  }

  @Test
  public void testThat_orConditionEvaluationWorks() throws Exception {
    OrCondition or = new OrCondition();

    // empty list -> no match
    assertThat(or.matches(ctx)).isFalse();

    // one positive match
    or.addCondition(ALWAYS);
    assertThat(or.matches(ctx)).isTrue();

    // two positive matches
    or.addCondition(ALWAYS);
    assertThat(or.matches(ctx)).isTrue();

    // one negative, two positive
    or.addCondition(NEVER);
    assertThat(or.matches(ctx)).isTrue();

    // one negative only
    or.getConditions().clear();
    or.addCondition(NEVER);
    assertThat(or.matches(ctx)).isFalse();
  }
  
  @Test
  public void testThat_existsConditionEvaluationWorks() throws Exception {
    given(ctx.resolveValue(Specifier.from("foo.bar"))).willReturn("baz");
    given(ctx.resolveValue(Specifier.from("foo.baz"))).willReturn("baz");

    AbstractTargetListCondition<?> exists = new ExistsCondition();

    // found
    exists.withTarget("foo.bar");
    assertThat(exists.matches(ctx)).isTrue();

    // two found
    exists.withTarget("foo.baz");
    assertThat(exists.matches(ctx)).isTrue();

    // not found
    exists.withTarget("something");
    assertThat(exists.matches(ctx)).isFalse();
  }
  

  @Test
  public void testThat_notConditionEvaluationWorks() throws Exception {
    NotCondition not = new NotCondition();

    not.addCondition(ALWAYS);
    assertThat(not.matches(ctx)).isFalse();

    not.getConditions().clear();
    not.addCondition(NEVER);
    assertThat(not.matches(ctx)).isTrue();
  }
  
  @Test
  public void testThat_trueConditionEvaluationWorks() throws Exception {
    given(ctx.resolveValue(Specifier.from("foo.true"))).willReturn("true");
    given(ctx.resolveValue(Specifier.from("foo.TrUe"))).willReturn("TrUe");
    given(ctx.resolveValue(Specifier.from("foo._true"))).willReturn(" true");
    given(ctx.resolveValue(Specifier.from("foo.true_"))).willReturn("true ");
    given(ctx.resolveValue(Specifier.from("foo.false"))).willReturn("false");
    given(ctx.resolveValue(Specifier.from("foo.something"))).willReturn("something");
    
    TrueCondition trueC = new TrueCondition();
    trueC.withTarget("");
    
    Stream.of("foo.true", "foo.TrUe", "foo._true", "foo.true_").forEach(t -> {
      trueC.withTarget(0, t);
      assertThat(trueC.matches(ctx)).as("Target ref %s", t).isTrue();
    });
    
    Stream.of("foo.something", "foo.false", "foo.doesntexist").forEach(t -> {
      trueC.withTarget(0, t);
      assertThat(trueC.matches(ctx)).as("Target ref %s", t).isFalse();
    });
  }
  
  @Test
  public void testThat_falseConditionEvaluationWorks() throws Exception {
    given(ctx.resolveValue(Specifier.from("foo.true"))).willReturn("true");
    given(ctx.resolveValue(Specifier.from("foo.TrUe"))).willReturn("TrUe");
    given(ctx.resolveValue(Specifier.from("foo._true"))).willReturn(" true");
    given(ctx.resolveValue(Specifier.from("foo.true_"))).willReturn("true ");
    given(ctx.resolveValue(Specifier.from("foo.something"))).willReturn("something");
    
    FalseCondition falseC = new FalseCondition();
    falseC.withTarget("");
    
    Stream.of("foo.true", "foo.TrUe", "foo._true", "foo.true_").forEach(t -> {
      falseC.withTarget(0, t);
      assertThat(falseC.matches(ctx)).as("Target ref %s", t).isFalse();
    });
    

    Stream.of("foo.something", "foo.false", "foo.doesntexist").forEach(t -> {
      falseC.withTarget(0, t);
      assertThat(falseC.matches(ctx)).as("Target ref %s", t).isTrue();
    });
  }

  @Test
  public void testThat_rangeConditionEvaluationWorks() throws Exception {
    given(ctx.resolveValue(Specifier.from("foo.bar"))).willReturn("3");

    RangeCondition range = new RangeCondition();

    // positive match
    range.addPredicate("foo.bar", asList("2", "4"));
    assertThat(range.matches(ctx)).isTrue();

    // positive match (lower limit)
    range.getPredicates().clear();
    range.addPredicate("foo.bar", asList("3", "4"));
    assertThat(range.matches(ctx)).isTrue();

    // positive match (upper limit)
    range.getPredicates().clear();
    range.addPredicate("foo.bar", asList("2", "3"));
    assertThat(range.matches(ctx)).isTrue();

    // negative match (non-matching value)
    range.getPredicates().clear();
    range.addPredicate("foo.bar", asList("4", "5"));
    assertThat(range.matches(ctx)).isFalse();

    // negative match (no such value)
    range.getPredicates().clear();
    range.addPredicate("yada.yada", asList("yada!"));
    assertThat(range.matches(ctx)).isFalse();
  }
}
