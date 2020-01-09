package com.neverpile.eureka.plugin.audit.verification;

/**
 * Digital Signature Verifier.
 */
public interface DigSigVer {

  /**
   * Verifies a given digital signature for the provided data.
   *
   * @param encKey Encryption key.
   * @param sigToVerify Signature to be verified.
   * @param data Data to which the signature was created.
   * @return {@code true} if the signature is valid - {@code false} otherwise.
   */
  boolean verDigSig(byte[] encKey, byte[] sigToVerify, byte[] data) throws Exception;
}
