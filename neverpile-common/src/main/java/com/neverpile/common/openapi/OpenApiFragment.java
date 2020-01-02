package com.neverpile.common.openapi;

import java.io.IOException;
import java.io.InputStream;


/**
 * An OpenApiFragment represents a complete or partial OpenAPI specification (in YAML or JSON form)
 * for the endpoints of an application. By declaring OpenAPI fragments, an application can provide
 * its own OpenAPI specification, even if the application itself is rather dynamic and the available
 * endpoints are not necessarily known at compile time.
 * <p>
 * OpenAPI fragments are thus an alternative to fully dynamically generated specifications using
 * SpringFox etc. which has been found to be rather error-prone.
 */
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
   * Return the stream from which the OpenAPI stream can be loaded. The stream must contain a valid
   * (but possibly partial) OpenAPI specification in YAML or JSON form.
   * 
   * @return the fragment stream
   * @throws IOException upon problems reading or parsing the OpenAPI fragment
   */
  InputStream getFragmentStream() throws IOException;

}