package com.neverpile.openapi.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neverpile.common.openapi.OpenApiFragment;

@RestController
@RequestMapping(path = "/swagger-ui/swagger-ui-config.json", produces = MediaType.APPLICATION_JSON_VALUE)
public class SwaggerUIConfigurationResource {
  @Autowired(required = false)
  List<OpenApiFragment> apiFragments = new ArrayList<>(0);

  @Autowired
  ObjectMapper mapper;

  @GetMapping(path = "", produces = {
      MediaType.APPLICATION_JSON_VALUE, "application/json"
  })
  public ResponseEntity<JsonNode> applicationList() throws IOException {
    ObjectNode config = mapper.createObjectNode();

    ArrayNode urls = config.withArray("urls");
    apiFragments.stream() //
        .filter(f -> !f.getApplication().equals(OpenApiFragment.GLOBAL)) //
        .map(f -> f.getApplication()) //
        .distinct() //
        .forEach(a -> urls.addObject().put("name", a).put("url", "/openapi/" + a + ".json"));

    // return the application list
    return ResponseEntity.ok() //
        .contentType(MediaType.APPLICATION_JSON) //
        .body(config);
  }
}
