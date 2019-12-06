package com.neverpile.eureka.rest.configuration;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.DocumentFacet;

@Component
public class FacetedDocumentDtoModule extends SimpleModule {
  private static final Logger LOGGER = LoggerFactory.getLogger(FacetedDocumentDtoModule.class);
  
  private static final long serialVersionUID = 1L;

  @Autowired(required = false)
  private final List<DocumentFacet<?>> facets = Collections.emptyList();

  public FacetedDocumentDtoModule() {
    super(FacetedDocumentDtoModule.class.getSimpleName(), Version.unknownVersion());

    setDeserializerModifier(new FacetedDocumentDtoDeserializerModifier());
    
    new Exception("Module created").printStackTrace();
  }

  public class FacetedDocumentDtoDeserializerModifier extends BeanDeserializerModifier {
    public class FacetDeserializer extends BeanDeserializer {
      private static final long serialVersionUID = 1L;

      public FacetDeserializer(final BeanDeserializerBase base) {
        super(base);

      }

      @Override
      protected void handleUnknownProperty(final JsonParser p, final DeserializationContext ctxt,
          final Object beanOrClass, final String propName) throws IOException {
        facets.stream().filter(f -> f.getName().equals(propName)).forEach(f -> {
          LOGGER.info("Handled unknown property " + propName);
          try {
            JavaType valueType = f.getValueType(ctxt.getTypeFactory());
            JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(valueType);
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
    public JsonDeserializer<?> modifyDeserializer(final DeserializationConfig config, final BeanDescription beanDesc,
        final JsonDeserializer<?> deserializer) {
      if (beanDesc.getBeanClass() == DocumentDto.class) {
        LOGGER.info("Modified deserializer for DocumentDto");
        return new FacetDeserializer((BeanDeserializerBase) deserializer);
      }
      return deserializer;
    }
  }
}