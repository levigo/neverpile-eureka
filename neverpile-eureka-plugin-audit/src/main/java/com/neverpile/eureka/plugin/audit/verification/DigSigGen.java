package com.neverpile.eureka.plugin.audit.verification;

import java.security.SignatureException;

/**
 * Dgital Signature Generator.
 */
public interface DigSigGen {

  /**
   * Creates a signature for the given date.
   *
   * @param data the data to create the signature for.
   * @return the signature in bytes.
   * @throws SignatureException Error occured while creating the signature.
   */
  byte[] signData(byte[] data) throws SignatureException;

  /**
   * Get the public Key to verify the signature with.
   *
   * @return the bublic key for all creatd signature.
   */
  byte[] getPubKey();
}
