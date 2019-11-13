package com.neverpile.eureka.rest.configuration.springfox;

import static com.google.common.collect.Maps.*;
import static springfox.documentation.schema.Collections.*;
import static springfox.documentation.schema.Maps.*;
import static springfox.documentation.schema.ResolvedTypes.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.DocumentFacet;

import springfox.documentation.builders.ModelPropertyBuilder;
import springfox.documentation.schema.Model;
import springfox.documentation.schema.ModelProperty;
import springfox.documentation.schema.ModelReference;
import springfox.documentation.schema.ResolvedTypes;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.schema.plugins.SchemaPluginsManager;
import springfox.documentation.schema.property.ModelPropertiesProvider;
import springfox.documentation.spi.schema.SyntheticModelProviderPlugin;
import springfox.documentation.spi.schema.contexts.ModelContext;

@Component
@ConditionalOnBean(TypeResolver.class)
public class SpringfoxExposeFacetModels implements SyntheticModelProviderPlugin {

  @Autowired(required = false)
  private List<DocumentFacet<?>> facets;

  @Autowired
  private TypeResolver typeResolver;

  @Autowired
  private TypeNameExtractor typeNameExtractor;

  @Autowired
  @Qualifier("cachedModelProperties")
  private ModelPropertiesProvider propertiesProvider;

  @Autowired
  private SchemaPluginsManager schemaPluginsManager;

  private ResolvedType fromJackson(final JavaType t) {
    if (t.isArrayType())
      return typeResolver.arrayType(fromJackson(t.getContentType()));

    if (t.isCollectionLikeType())
      return typeResolver.resolve(List.class, fromJackson(t.getContentType()));

    if (t.isMapLikeType())
      return typeResolver.resolve(Map.class, fromJackson(t.getKeyType()), fromJackson(t.getContentType()));

    return typeResolver.resolve(t.getRawClass());
  }

  @Override
  public boolean supports(final ModelContext context) {
    return context.resolvedType(typeResolver).getErasedType().equals(DocumentDto.class);
  }

  private List<ModelProperty> properties(final ModelContext context, final ResolvedType propertiesHost) {
    List<ModelProperty> staticProperties = propertiesProvider.propertiesFor(propertiesHost, context);

    List<ModelProperty> properties = new ArrayList<>();
    properties.addAll(staticProperties);
    properties.addAll(facetProperties(context, staticProperties));
    return properties;
  }

  private Collection<? extends ModelProperty> facetProperties(final ModelContext context,
      final List<ModelProperty> staticProperties) {
    return facets.stream() //
        .map(f -> {
          JavaType valueType = f.getValueType(TypeFactory.defaultInstance());
          ModelProperty modelProperty = new ModelPropertyBuilder() //
              .name(f.getName()) //
              .type(fromJackson(valueType)) //
              .build();
          modelProperty.updateModelRef(ResolvedTypes.modelRefFactory(context, typeNameExtractor));

          return modelProperty;
        }) //
        // exclude existing static properties
        .filter(p -> !staticProperties.stream().anyMatch(p2 -> p2.getName().equals(p.getName()))) //
        .collect(Collectors.toList());
  }

  private Function<ModelProperty, String> byPropertyName() {
    return new Function<ModelProperty, String>() {
      @Override
      public String apply(final ModelProperty input) {
        return input.getName();
      }
    };
  }

  @Override
  public Model create(final ModelContext modelContext) {
    ResolvedType propertiesHost = modelContext.alternateFor(modelContext.resolvedType(typeResolver));

    ImmutableMap<String, ModelProperty> propertiesIndex = uniqueIndex(properties(modelContext, propertiesHost),
        byPropertyName());

    Map<String, ModelProperty> properties = newTreeMap();
    properties.putAll(propertiesIndex);

    String typeName = typeNameExtractor.typeName(ModelContext.fromParent(modelContext, propertiesHost));
    modelContext.getBuilder().id(typeName).type(propertiesHost).name(typeName).qualifiedType(
        simpleQualifiedTypeName(propertiesHost)).properties(properties).description("").baseModel("").discriminator(
            "").subTypes(new ArrayList<ModelReference>());

    return schemaPluginsManager.model(modelContext);
  }

  @Override
  public List<ModelProperty> properties(final ModelContext context) {
    return Collections.emptyList();
  }

  @Override
  public Set<ResolvedType> dependencies(final ModelContext context) {
    return facets.stream() //
        .flatMap(f -> unwrapCollections(fromJackson(f.getValueType(TypeFactory.defaultInstance())))) //
        .collect(Collectors.toSet());
  }

  private Stream<ResolvedType> unwrapCollections(final ResolvedType t) {
    if(isContainerType(t))
      return Stream.of(collectionElementType(t));
    if(isMapType(t))
      return Stream.of(t.typeParametersFor(Map.class).get(0), t.typeParametersFor(Map.class).get(1));
    if(t.isArray())
      return Stream.of(t.getArrayElementType());
    return Stream.of(t);
  }
}
