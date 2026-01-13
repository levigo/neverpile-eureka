package com.neverpile.eureka.rest.configuration;

import java.io.Serial;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.core.JsonParser;
import tools.jackson.core.Version;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.bean.BeanDeserializer;
import tools.jackson.databind.deser.bean.BeanDeserializerBase;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.module.SimpleModule;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.DocumentFacet;

@Component
public class FacetedDocumentDtoModule extends SimpleModule {
  @Serial
  private static final long serialVersionUID = 1L;

  @Autowired
  ApplicationContext appContext;

  public FacetedDocumentDtoModule() {
    super(FacetedDocumentDtoModule.class.getSimpleName(), Version.unknownVersion());

    setDeserializerModifier(new FacetedDocumentDtoDeserializerModifier());
  }

  public class FacetedDocumentDtoDeserializerModifier extends ValueDeserializerModifier {
    @SuppressWarnings("rawtypes")
    public class FacetDeserializer extends BeanDeserializer {
      @Serial
      private static final long serialVersionUID = 1L;

      private final Collection<DocumentFacet> facets;

      public FacetDeserializer(final BeanDeserializer base, final Collection<DocumentFacet> facets) {
        super(base);
        this.facets = facets;

      }

      @Override
      protected void handleUnknownProperty(final JsonParser p, final DeserializationContext ctxt,
          final Object beanOrClass, final String propName) {
        facets.stream().filter(f -> f.getName().equals(propName)).forEach(f -> {
          try {
            JavaType valueType = f.getValueType(ctxt.getTypeFactory());
            ValueDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(valueType);
            Object value = deserializer.deserialize(p, ctxt);
            ((DocumentDto) beanOrClass).setFacet(f.getName(), value);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
        super.handleUnknownProperty(p, ctxt, beanOrClass, propName);
      }
    }

    @Override
    public ValueDeserializer<?> modifyDeserializer(final DeserializationConfig config, final BeanDescription.Supplier beanDescRef, final ValueDeserializer<?> deserializer) {
      if (beanDescRef.getBeanClass() == DocumentDto.class
            && deserializer instanceof BeanDeserializer) {
        @SuppressWarnings("rawtypes")
        Collection<DocumentFacet> facets = appContext.getBeansOfType(DocumentFacet.class).values();
        return new FacetDeserializer((BeanDeserializer) deserializer, facets);
      }
      return deserializer;
    }
  }
}