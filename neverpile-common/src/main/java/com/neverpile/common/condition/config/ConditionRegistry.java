package com.neverpile.common.condition.config;

import java.util.Map;

import com.neverpile.common.condition.Condition;

/**
 * A condition registry assigns names to {@link Condition} implementation classes, allowing the discovery of available condition implementations.
 */
public interface ConditionRegistry {
  Map<String, Class<? extends Condition>> getConditions();
}
