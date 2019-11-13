package com.neverpile.authorization.policy;

import io.swagger.annotations.ApiModelProperty;

/**
 * An enum listing the possible outcomes of a authorization check.
 */
public enum Effect {
  /**
   * Allow the operation  
   */
  @ApiModelProperty("Allow the operation")
  ALLOW,
  
  /**
   * Deny the operation
   */
  @ApiModelProperty("Deny the operation")
  DENY
}
