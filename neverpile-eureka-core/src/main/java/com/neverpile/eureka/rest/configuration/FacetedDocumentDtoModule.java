package com.neverpile.eureka.rest.configuration;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
  private static final long serialVersionUID = 1L;

  @Autowired(required = false)
  private final List<DocumentFacet<?>> facets = Collections.emptyList();

  public FacetedDocumentDtoModule() {
    super(FacetedDocumentDtoModule.class.getSimpleName(), Version.unknownVersion());

//    setSerializerModifier(new FacetedDocumentDtoSerializerModifier());
    setDeserializerModifier(new FacetedDocumentDtoDeserializerModifier());
  }

//  public class FacetedDocumentDtoSerializerModifier extends BeanSerializerModifier {
//    public class FacetSerializer extends BeanSerializer {
//      private static final long serialVersionUID = 1L;
//
//      public FacetSerializer(final BeanSerializer serializer) {
//        super(serializer);
//      }
//
//      @Override
//      protected void serializeFields(final Object bean, final JsonGenerator gen, final SerializerProvider provider)
//          throws IOException {
//        super.serializeFields(bean, gen, provider);
//
//        DocumentDto dto = (DocumentDto) bean;
//        facets.forEach(f -> {
//          Object value = dto.getFacets().get(f.getName());
//          if (null != value) {
//            try {
//              gen.writeObjectField(f.getName(), value);
//            } catch (Exception e) {
//              // TODO Auto-generated catch block
//              e.printStackTrace();
//            }
//          }
//        });
//      }
//    }
//
//    @Override
//    public JsonSerializer<?> modifySerializer(final SerializationConfig config, final BeanDescription beanDesc,
//        final JsonSerializer<?> serializer) {
//      if (beanDesc.getBeanClass() == DocumentDto.class) {
//        return new FacetSerializer((BeanSerializer) serializer);
//      }
//      return serializer;
//    }
//  }

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
          try {
            JavaType valueType = f.getValueType(ctxt.getTypeFactory());
            JsonDeserializer<Object> deserializer = ctxt.findRootValueDeserializer(valueType);
            Object value = deserializer.deserialize(p, ctxt);
            ((DocumentDto) beanOrClass).setFacet(f.getName(), value);
          } catch (Exception e) {
            // e.printStackTrace();
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
        return new FacetDeserializer((BeanDeserializerBase) deserializer);
      }
      return deserializer;
    }
  }
}