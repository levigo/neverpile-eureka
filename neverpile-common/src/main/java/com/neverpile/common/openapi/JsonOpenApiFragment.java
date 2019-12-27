package com.neverpile.common.openapi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A JSON-node-based implementation of {@link OpenApiFragment}.
 */
public class JsonOpenApiFragment implements OpenApiFragment {
  private final String application;

  private final String name;

  private ObjectNode root = new ObjectMapper().createObjectNode();

  /**
   * Create a global fragment with the given name. Add contents to it by setting the root node
   * ({@link #setRoot(ObjectNode)}) or {@link #getRoot()} and adding children.
   * 
   * @param name the fragment name
   */
  public JsonOpenApiFragment(final String name) {
    this(GLOBAL, name);
  }

  /**
   * Create a fragment with the given application, and name. Add contents to it by setting the root
   * node ({@link #setRoot(ObjectNode)}) or {@link #getRoot()} and adding children.
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

  /**
   * Get the root node of the fragment. The root node is initially just and {@link ObjectNode} with
   * no children.
   * 
   * @return the root node
   */
  public ObjectNode getRoot() {
    return root;
  }

  /**
   * Set the root node of the fragment to the given {@link ObjectNode}.
   * 
   * @param root the root node
   */
  public void setRoot(final ObjectNode root) {
    this.root = root;
  }
}
