package com.neverpile.eureka.plugin.audit.verification;

public interface DigSigGen {
  byte[] signData(byte[] data) throws Exception;

  byte[] getPubKey();
}
