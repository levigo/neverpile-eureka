package com.neverpile.eureka.plugin.metadata.autoconfig;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.classmate.TypeResolver;
import com.neverpile.eureka.plugin.metadata.rest.MetadataDto;

import springfox.documentation.schema.property.ModelPropertiesProvider;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelContext;

public class SpringfoxEnrichMetadataDtoModel implements ModelBuilderPlugin {
  /**
   * @deprecated This is used as a Springfox surrogate only!
   */
  @Deprecated
  public static class MetadataElement {
    
  }
  
  @Autowired
  private TypeResolver typeResolver;

  @Autowired
  @Qualifier("cachedModelProperties")
  private ModelPropertiesProvider propertiesProvider;

  @Override
  public boolean supports(final DocumentationType delimiter) {
    return true;
  }

  @Override
  public void apply(final ModelContext context) {
    if(context.resolvedType(typeResolver).getErasedType().equals(MetadataDto.class)) {
      context.getBuilder().type(typeResolver.resolve(Map.class, String.class, MetadataElement.class));
    }
  }
}
