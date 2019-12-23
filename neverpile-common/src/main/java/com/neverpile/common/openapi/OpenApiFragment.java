package com.neverpile.common.openapi;

import java.io.IOException;
import java.io.InputStream;


public interface OpenApiFragment {
  public static final String GLOBAL = "GLOBAL";

  /**
   * Return the name of the application, service, etc. to which the fragment belongs. Return
   * {@value #GLOBAL} for fragments that belong to <em>all</em> applications within the container.
   * This can be used e.g. for authentication-related information that applies to all endpoints.
   * 
   * @return the name of the application
   */
  String getApplication();

  /**
   * Return the name of the fragment.
   * 
   * @return the name of the fragment
   */
  String getName();

  /**
   * Return the stream from which the OpenAPI stream can be loaded.
   * 
   * @return the fragment stream
   * @throws IOException 
   */
  InputStream getFragmentStream() throws IOException;

}