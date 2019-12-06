package com.neverpile.authorization.policy;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * An enum listing the possible outcomes of a authorization check.
 */
public enum Effect {
  /**
   * Allow the operation  
   */
  @Schema(description = "Allow the operation")
  ALLOW,
  
  /**
   * Deny the operation
   */
  @Schema(description = "Deny the operation")
  DENY
}
