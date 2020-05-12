package com.neverpile.eureka.api;

import com.neverpile.common.authorization.api.Action;
import com.neverpile.common.authorization.api.AuthorizationService;
import com.neverpile.common.authorization.api.CoreActions;
import com.neverpile.eureka.model.Document;

/**
 * A DocumentAuthorizationService is specialized in {@link Document}-related access decisions.
 */
public interface DocumentAuthorizationService {

  /**
   * Authorize the given action targeting the given resource path below the document resource.
   * Implementations typically delegate to an {@link AuthorizationService}, providing it with
   * context information from the given document.
   *
   * @param document        the document which the access is related to
   * @param action          the action to authorize
   * @param subResourcePath the sub-resource path. Multiple levels may be specified as separate
   *                        arguments.
   * @return the access decision: <code>true</code> if the access shall be allowed
   */
  boolean authorizeSubResourceAction(Document document, Action action, String... subResourcePath);

  /**
   * Authorize a create action on the given document sub-resource. Implementations typically
   * delegate to an {@link AuthorizationService}, providing it with context information from the
   * given document.
   *
   * @param document        the document which the access is related to
   * @param subResourcePath the sub-resource path. Multiple levels may be specified as separate
   *                        arguments.
   * @return the access decision: <code>true</code> if the access shall be allowed
   */
  default boolean authorizeSubResourceCreate(final Document document, final String... subResourcePath) {
    return authorizeSubResourceAction(document, CoreActions.CREATE, subResourcePath);
  }

  /**
   * Authorize a retrieve action on the given document sub-resource. Implementations typically
   * delegate to an {@link AuthorizationService}, providing it with context information from the
   * given document.
   *
   * @param document        the document which the access is related to
   * @param subResourcePath the sub-resource path. Multiple levels may be specified as separate
   *                        arguments.
   * @return the access decision: <code>true</code> if the access shall be allowed
   */
  default boolean authorizeSubResourceGet(final Document document, final String... subResourcePath) {
    return authorizeSubResourceAction(document, CoreActions.GET, subResourcePath);
  }

  /**
   * Authorize an update action on the given document sub-resource. Implementations typically
   * delegate to an {@link AuthorizationService}, providing it with context information from the
   * given document.
   *
   * @param document        the document which the access is related to
   * @param subResourcePath the sub-resource path. Multiple levels may be specified as separate
   *                        arguments.
   * @return the access decision: <code>true</code> if the access shall be allowed
   */
  default boolean authorizeSubResourceUpdate(final Document document, final String... subResourcePath) {
    return authorizeSubResourceAction(document, CoreActions.UPDATE, subResourcePath);
  }

  /**
   * Authorize a deletion action on the given document sub-resource. Implementations typically
   * delegate to an {@link AuthorizationService}, providing it with context information from the
   * given document.
   *
   * @param document        the document which the access is related to
   * @param subResourcePath the sub-resource path. Multiple levels may be specified as separate
   *                        arguments.
   * @return the access decision: <code>true</code> if the access shall be allowed
   */
  default boolean authorizeSubResourceDelete(final Document document, final String... subResourcePath) {
    return authorizeSubResourceAction(document, CoreActions.DELETE, subResourcePath);
  }

}
