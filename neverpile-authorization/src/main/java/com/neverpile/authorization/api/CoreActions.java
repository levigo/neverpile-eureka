package com.neverpile.authorization.api;

/**
 * An enum of actions most likely to be useful in almost any application. All action keys use the
 * namespace-prefix <code>core:</code> (see {@link #NAMESPACE}).
 */
public enum CoreActions implements Action {
  /**
   * A generic get-action used in conjunction with the retrieval of some resource (HTTP GET verb).
   */
  GET,

  /**
   * A generic get-action used in conjunction with the creation of some resource (HTTP POST and PUT
   * verbs).
   */
  CREATE,

  /**
   * A generic update-action used in conjunction with the update of some resource (HTTP PUT verb).
   */
  UPDATE,

  /**
   * A generic delete-action used in conjunction with the deletion of some resource (HTTP DELETE
   * verb).
   */
  DELETE,

  /**
   * A generic query-action used in conjunction with a search/query for resources matching some
   * condition (HTTP GET verb).
   */
  QUERY, 
  
  /**
   * A generic validation-action used in conjunction with validation of a set of data (HTTP GET and POST verbs).
   */
  VALIDATE;

  public static final String NAMESPACE = "core";

  @Override
  public String key() {
    return NAMESPACE + ":" + name();
  }

}
