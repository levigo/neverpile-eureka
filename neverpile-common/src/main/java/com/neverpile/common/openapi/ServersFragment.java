package com.neverpile.common.openapi;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class ServersFragment extends JsonOpenApiFragment {

  private final ArrayNode serversNode;

  public ServersFragment(final String name) {
    super(name);
    serversNode = getRoot().withArray("servers");
  }
  
  public ServersFragment withServer(final String url, final String description) {
    getServersNode().addObject().put("url", url).put("description", description);
    return this;
  }

  public ArrayNode getServersNode() {
    return serversNode;
  }
}
