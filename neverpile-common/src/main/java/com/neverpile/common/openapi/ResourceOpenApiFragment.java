package com.neverpile.common.openapi;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;

/**
 * A Spring-resource-based implementation of {@link OpenApiFragment}.
 */
public class ResourceOpenApiFragment implements OpenApiFragment {
  private final String application;

  private final String name;

  private final Resource resource;

  /**
   * Create a global fragment with the given name and resource.
   * 
   * @param name the fragment name
   * @param resource the resource
   */
  public ResourceOpenApiFragment(final String name, final Resource resource) {
    this(GLOBAL, name, resource);
  }

  /**
   * Create a fragment with the given application, name and resource.
   * 
   * @param application the application name
   * @param name the fragment name
   * @param resource the resource
   */
  public ResourceOpenApiFragment(final String application, final String name, final Resource resource) {
    this.application = application;
    this.name = name;
    this.resource = resource;
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
    return resource.getInputStream();
  }
}
