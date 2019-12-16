package com.neverpile.eureka.plugin.audit.verification;

public interface DigSigVer {
  boolean verDigSig(byte[] encKey, byte[] sigToVerify, byte[] data) throws Exception;
}
