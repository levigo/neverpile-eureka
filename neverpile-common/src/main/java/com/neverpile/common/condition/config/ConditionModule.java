package com.neverpile.common.condition.config;

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.neverpile.common.condition.CompositeCondition;
import com.neverpile.common.condition.Condition;
import com.neverpile.common.condition.CoreConditionRegistry;

/**
 * A Jackson {@link Module} extending Jackson with capabilities for the marshalling and
 * unmarshalling of {@link Condition}s.
 * <p>
 * The {@link Condition} type hierarchy is supposed to be extensible. To that end, a
 * {@link ConditionRegistry} provides a mapping from a condition name to the corresponding
 * {@link Condition}-implementation. This module picks up all provided registries and uses them to
 * resolve the condition implementations during marshalling and unmarshalling.
 */
@Component
@Import(CoreConditionRegistry.class)
public class ConditionModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  @Autowired(required = false)
  private final List<ConditionRegistry> conditionRegistries = Collections.emptyList();

  private Map<String, Class<? extends Condition>> conditionClassByName = new HashMap<>();
  private Map<Class<? extends Condition>, String> conditionNameByClass = new HashMap<>();

  public ConditionModule() {
    super(ConditionModule.class.getSimpleName(), Version.unknownVersion());

    setSerializerModifier(new ConditionSerializerModifier());
    setDeserializerModifier(new ConditionDeserializerModifier());
  }

  @PostConstruct
  private void init() {
    conditionClassByName = conditionRegistries.stream() //
        .flatMap(r -> r.getConditions().entrySet().stream()) //
        .collect(toMap(e -> e.getKey(), e -> e.getValue()));

    conditionNameByClass = conditionClassByName.entrySet().stream() //
        .collect(toMap(e -> e.getValue(), e -> e.getKey())); // invert mapping
  }

  public class ConditionSerializerModifier extends BeanSerializerModifier {
    public class CompositeConditionSerializer extends BeanSerializer {
      private static final long serialVersionUID = 1L;

      public CompositeConditionSerializer(final BeanSerializer serializer) {
        super(serializer);
      }

      @Override
      protected void serializeFields(final Object bean, final JsonGenerator gen, final SerializerProvider provider)
          throws IOException {
        super.serializeFields(bean, gen, provider);

        CompositeCondition<?> dto = (CompositeCondition<?>) bean;
        for (Condition c : dto.getConditions()) {
          String name = conditionNameByClass.get(c.getClass());

          if (null == name)
            throw new IllegalArgumentException(
                "Cannot serialize condition of type " + c.getClass() + ": name cannot be resolved");

          provider.defaultSerializeField(name, c, gen);
        }
      }
    }

    @Override
    public JsonSerializer<?> modifySerializer(final SerializationConfig config, final BeanDescription beanDesc,
        final JsonSerializer<?> serializer) {
      if (CompositeCondition.class.isAssignableFrom(beanDesc.getBeanClass())) {
        return new CompositeConditionSerializer((BeanSerializer) serializer);
      }
      return serializer;
    }
  }

  public class ConditionDeserializerModifier extends BeanDeserializerModifier {
    public class CompositeConditionDeserializer extends BeanDeserializer {
      private static final long serialVersionUID = 1L;

      public CompositeConditionDeserializer(final BeanDeserializerBase base) {
        super(base);

      }

      @Override
      protected void handleUnknownProperty(final JsonParser p, final DeserializationContext ctxt,
          final Object beanOrClass, final String propName) throws IOException {
        Class<? extends Condition> conditionClass = conditionClassByName.get(propName);
        if (null == conditionClass)
          throw UnrecognizedPropertyException.from(p, beanOrClass, propName,
              new ArrayList<>(conditionClassByName.keySet()));
        else {
          JavaType valueType = ctxt.getTypeFactory().constructType(conditionClass);
          JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(valueType);
          Object value = deserializer.deserialize(p, ctxt);
          ((CompositeCondition<?>) beanOrClass).addCondition((Condition) value);
        }
      }
    }

    @Override
    public JsonDeserializer<?> modifyDeserializer(final DeserializationConfig config, final BeanDescription beanDesc,
        final JsonDeserializer<?> deserializer) {
      if (CompositeCondition.class.isAssignableFrom(beanDesc.getBeanClass())) {
        return new CompositeConditionDeserializer((BeanDeserializerBase) deserializer);
      }
      return deserializer;
    }
  }
}