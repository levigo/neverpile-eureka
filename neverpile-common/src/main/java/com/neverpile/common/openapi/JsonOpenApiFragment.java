package com.neverpile.common.openapi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A JSON-node-based implementation of {@link OpenApiFragment}.
 */
public class JsonOpenApiFragment implements OpenApiFragment {
  private final String application;

  private final String name;
  
  private JsonNode root = new ObjectMapper().createObjectNode();

  /**
   * Create a global fragment with the given name and resource.
   * 
   * @param name the fragment name
   */
  public JsonOpenApiFragment(final String name) {
    this(GLOBAL, name);
  }

  /**
   * Create a fragment with the given application, name and resource.
   * 
   * @param application the application name
   * @param name the fragment name
   */
  public JsonOpenApiFragment(final String application, final String name) {
    this.application = application;
    this.name = name;
  }

  @Override
  public String getApplication() {
    return application;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public InputStream getFragmentStream() throws IOException {
    return new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(root));
  }

  public JsonNode getRoot() {
    return root;
  }

  public void setRoot(final JsonNode root) {
    this.root = root;
  }
}
