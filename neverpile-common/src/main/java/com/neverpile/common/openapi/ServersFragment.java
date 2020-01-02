package com.neverpile.common.openapi;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * An {@link OpenApiFragment} used to declare {@code servers}s at which the endpoints are exposed.
 * See {@link #withServer(String, String)} and <a
 * href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.2.md#serverObject">the
 * specification</a> for details.
 */
public class ServersFragment extends JsonOpenApiFragment {

  private final ArrayNode serversNode;

  public ServersFragment(final String name) {
    super(name);
    serversNode = getRoot().withArray("servers");
  }

  /**
   * Declare that the exposed endpoints will be available at the given server URL prefix.
   * <p>
   * Example usage under Spring:
   * <pre>
   * &#64;Bean
   * public OpenApiFragment serversOpenApiFragment() throws IOException {
   *   return new ServersFragment("servers").withServer("http://example.com/", "my service");
   * }
   * </pre>
   * 
   * @param url the URL prefix
   * @param description the service description
   * @return a servers fragment
   */
  public ServersFragment withServer(final String url, final String description) {
    getServersNode().addObject().put("url", url).put("description", description);
    return this;
  }

  public ArrayNode getServersNode() {
    return serversNode;
  }
}
