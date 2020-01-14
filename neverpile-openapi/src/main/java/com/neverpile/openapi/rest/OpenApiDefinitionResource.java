package com.neverpile.openapi.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.neverpile.common.openapi.OpenApiFragment;

@RestController
@RequestMapping(path = "/openapi", produces = {
    MediaType.APPLICATION_JSON_VALUE, "application/x-yaml"
})
public class OpenApiDefinitionResource {
  @Autowired(required = false)
  List<OpenApiFragment> apiFragments = new ArrayList<>(0);

  @GetMapping(path = "", produces = {
      MediaType.APPLICATION_JSON_VALUE, "application/json"
  })
  public ResponseEntity<List<String>> applicationList() throws IOException {
    // return the application list
    return ResponseEntity.ok() //
        .contentType(MediaType.APPLICATION_JSON) //
        .body(apiFragments.stream() //
            .filter(f -> !f.getApplication().equals(OpenApiFragment.GLOBAL)) //
            .map(f -> f.getApplication()) //
            .distinct() //
            .collect(Collectors.toList()) //
        );
  }

  @GetMapping(path = "/{application:[^\\.]+}{extension:(?:\\.(?:json|yaml|yml))?}", produces = {
      MediaType.APPLICATION_JSON_VALUE, "application/x-yaml"
  })
  public ResponseEntity<?> mergedDefinitions(@RequestHeader("Accept") final List<String> accept,
      @PathVariable final String application, @PathVariable(required = false) final String extension)
      throws IOException {
    // filter fragments by application
    List<OpenApiFragment> filteredFragments = apiFragments.stream() //
        .filter(f -> f.getApplication().equals(application) || f.getApplication().equals(OpenApiFragment.GLOBAL)) //
        .collect(Collectors.toList());

    // if there are no or only GLOBAL fragments, return a 404
    if (!filteredFragments.stream().anyMatch(f -> !f.getName().equals(OpenApiFragment.GLOBAL)))
      return ResponseEntity.notFound().build();

    // merge fragments
    ObjectNode merged = new OpenApiDefinitionMerger().mergeFragments(filteredFragments);

    // do we want YAML?
    if ((!accept.contains(MediaType.APPLICATION_JSON_VALUE) && accept.contains("application/x-yaml"))
        || ".yaml".equals(extension) || ".yml".equals(extension)) {
      return ResponseEntity.ok() //
          .contentType(MediaType.parseMediaType("application/x-yaml")) //
          .body(new ObjectMapper( //
              new YAMLFactory() //
                  .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) //
                  .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) //
          ).writeValueAsString(merged));
    }

    // otherwise just return JSON
    return ResponseEntity.ok() //
        .contentType(MediaType.APPLICATION_JSON) //
        .body(new ObjectMapper().writeValueAsString(merged));
  }
}
