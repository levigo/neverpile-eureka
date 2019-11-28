package com.neverpile.eureka.rest.configuration.springfox;

import java.lang.reflect.Field;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelPropertyContext;

@Component
public class HideHATEOASLinks implements ModelPropertyBuilderPlugin {

  @Override
  public boolean supports(final DocumentationType delimiter) {
    return true;
  }

  @Override
  public void apply(final ModelPropertyContext context) {
    if (context.getBeanPropertyDefinition().isPresent()) {
      BeanPropertyDefinition b = context.getBeanPropertyDefinition().get();
      AnnotatedField annotatedField = b.getField();
      if (null != annotatedField) {
        Field field = (Field) annotatedField.getMember();
        if (field != null)
          hideLinksField(context, field);
      }
    } else if (context.getAnnotatedElement().isPresent() && context.getAnnotatedElement().get() instanceof Field) {
      Field f = (Field) context.getAnnotatedElement().get();
      hideLinksField(context, f);
    }
  }

  private void hideLinksField(final ModelPropertyContext context, final Field field) {
    if (RepresentationModel.class.equals(field.getDeclaringClass()) && field.getName().contains("links")) {
      context.getBuilder().isHidden(true);
    }
  }

}
