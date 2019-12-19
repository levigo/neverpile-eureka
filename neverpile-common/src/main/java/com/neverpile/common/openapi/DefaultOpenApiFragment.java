package com.neverpile.common.openapi;

import org.springframework.core.io.Resource;

/**
 * A simple default implementation of {@link OpenApiFragment}.
 */
public class DefaultOpenApiFragment implements OpenApiFragment {
  private final String application;

  private final String name;

  private final Resource resource;

  /**
   * Create a global fragment with the given name and resource.
   * 
   * @param name the fragment name
   * @param resource the resource
   */
  public DefaultOpenApiFragment(final String name, final Resource resource) {
    this(GLOBAL, name, resource);
  }

  /**
   * Create a fragment with the given application, name and resource.
   * 
   * @param application the application name
   * @param name the fragment name
   * @param resource the resource
   */
  public DefaultOpenApiFragment(final String application, final String name, final Resource resource) {
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
  public Resource getResource() {
    return resource;
  }
}
