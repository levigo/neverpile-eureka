package com.neverpile.eureka.plugin.audit.verification;

import java.security.SignatureException;

/**
 * Digital Signature Generator.
 */
public interface DigSigGen {

  /**
   * Creates a signature for the given date.
   *
   * @param data the data to create the signature for.
   * @return the signature in bytes.
   * @throws SignatureException Error occurred while creating the signature.
   */
  byte[] signData(byte[] data) throws SignatureException;

  /**
   * Get the public Key to verify the signature with.
   *
   * @return the public key for all created signature.
   */
  byte[] getPubKey();
}
