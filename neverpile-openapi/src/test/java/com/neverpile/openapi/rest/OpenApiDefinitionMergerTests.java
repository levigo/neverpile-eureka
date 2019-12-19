package com.neverpile.openapi.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.neverpile.common.openapi.DefaultOpenApiFragment;
import com.neverpile.openapi.rest.OpenApiDefinitionMerger;

public class OpenApiDefinitionMergerTests {
  @Test
  public void testThat_scalarValueMergeWorks() throws IOException {
    String mergedYaml = performMerge( //
        "foo: bar", //
        "bar: baz");

    assertThat(mergedYaml).isEqualTo("foo: bar\nbar: baz\n");
  }

  @Test
  public void testThat_firstScalarWins() throws IOException {
    String mergedYaml = performMerge( //
        "foo: bar", //
        "foo: baz");

    assertThat(mergedYaml).isEqualTo("foo: bar\n");
  }

  @Test
  public void testThat_nestedScalarMergeWorks() throws IOException {
    // @formatter:off
    String mergedYaml = performMerge(
      "foo:\n" + 
      " bar: baz",
      "foo:\n" + 
      "  yada: yada"
    );

    assertThat(mergedYaml).isEqualTo(
        "foo:\n"
        + "  bar: baz\n"
        + "  yada: yada\n"
    );
    // @formatter:on
  }
  
  @Test
  public void testThat_arrayMergeWorks() throws IOException {
    // @formatter:off
    String mergedYaml = performMerge(
        "foo:\n" + 
        "- a\n" +
        "- b\n",
        "foo:\n" + 
        "- c\n" +
        "- d\n"
    );
    
    assertThat(mergedYaml).isEqualTo(
        "foo:\n" +
        "- a\n" +
        "- b\n" +
        "- c\n" +
        "- d\n"
    );
    // @formatter:on
  }
  
  @Test
  public void testThat_arrayIncompatibleTypeIsRejected() throws IOException {
    String mergedYaml;
    // @formatter:off
    mergedYaml = performMerge(
        "foo:\n" + 
        "- a\n" +
        "- b\n",
        "foo: bar\n"
    );
    
    assertThat(mergedYaml).isEqualTo(
        "foo:\n" +
        "- a\n" +
        "- b\n"
    );
    // @formatter:on
  }

  private String performMerge(final String... yaml) throws IOException, JsonProcessingException {
    return new ObjectMapper(new YAMLFactory() //
        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) //
        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) //
    ) //
        .writeValueAsString( //
            new OpenApiDefinitionMerger().mergeFragments(Stream //
                .of(yaml) //
                .map(y -> new DefaultOpenApiFragment("foo", new ByteArrayResource(y.getBytes()))) //
                .collect(Collectors.toList()) //
            ) //
        );
  }
}
