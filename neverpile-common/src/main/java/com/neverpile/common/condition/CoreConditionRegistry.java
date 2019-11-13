package com.neverpile.common.condition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.neverpile.common.condition.config.ConditionRegistry;

/**
 * A {@link ConditionRegistry} for all core condition implementations.
 */
@Component
public class CoreConditionRegistry implements ConditionRegistry {
  private final Map<String, Class<? extends Condition>> conditions;

  public CoreConditionRegistry() {
    Map<String, Class<? extends Condition>> c = new HashMap<>();

    c.put("equals", EqualsCondition.class);
    c.put("and", AndCondition.class);
    c.put("or", OrCondition.class);
    c.put("exists", ExistsCondition.class);
    c.put("not", NotCondition.class);
    c.put("true", TrueCondition.class);
    c.put("false", FalseCondition.class);
    c.put("lessThan", LessThanCondition.class);
    c.put("lessOrEqualTo", LessOrEqualToCondition.class);
    c.put("greaterThan", GreaterThanCondition.class);
    c.put("greaterOrEqualTo", GreaterOrEqualToCondition.class);
    c.put("between", RangeCondition.class);

    conditions = Collections.unmodifiableMap(c);
  }

  @Override
  public Map<String, Class<? extends Condition>> getConditions() {
    return conditions;
  }

}
