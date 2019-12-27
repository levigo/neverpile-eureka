package com.neverpile.common.openapi;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * An {@link OpenApiFragment} used to declare {@code securityScheme}s and endpoint {@code security}.
 * See {@link #withBasicAuth()} and <a
 * href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#securitySchemeObject">the
 * specification</a> for usage details.
 */
public class SecurityFragment extends JsonOpenApiFragment {
  
  private final ObjectNode schemesNode;
  private final ArrayNode securityNode;

  public SecurityFragment(final String name) {
    super(name);
    schemesNode = getRoot().with("components").with("securitySchemes");
    securityNode = getRoot().withArray("security");
  }

  /**
   * Declare an HTTP basic auth security scheme and reference it for all exposed endponts.
   * <p>
   * Example usage in Spring:
   * 
   * <pre>
   * &#64;Bean
   * public OpenApiFragment securitySchemeFragment() {
   *   return new SecurityFragment("security").withBasicAuth();
   * }
   * </pre>
   * 
   * @return a security fragment
   */
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
