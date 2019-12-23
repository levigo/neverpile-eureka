package com.neverpile.common.openapi;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SecurityFragment extends JsonOpenApiFragment {


  private final ObjectNode schemesNode;
  private final ArrayNode securityNode;

  public SecurityFragment(final String name) {
    super(name);
    schemesNode = getRoot().with("components").with("securitySchemes");
    securityNode = getRoot().withArray("security");

    
  }
  
  public SecurityFragment withBasicAuth() {
    getSchemesNode().with("basicAuth").put("type", "http").put("scheme", "basic");
    getSecurityNode().addObject().withArray("basicAuth");
    return this;
  }

  public ObjectNode getSchemesNode() {
    return schemesNode;
  }

  public ArrayNode getSecurityNode() {
    return securityNode;
  }
}
