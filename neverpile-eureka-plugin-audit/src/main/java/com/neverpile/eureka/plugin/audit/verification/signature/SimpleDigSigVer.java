package com.neverpile.eureka.plugin.audit.verification.signature;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.springframework.stereotype.Service;

import com.neverpile.eureka.plugin.audit.verification.DigSigVer;

@Service
public class SimpleDigSigVer implements DigSigVer {
  @Override
  public boolean verDigSig(byte[] encKey, byte[] sigToVerify, byte[] data)
      throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
      SignatureException {
    // Get pub key.
    X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encKey);
    KeyFactory keyFactory = KeyFactory.getInstance("DSA", "SUN");
    PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);

    //
    Signature sig = Signature.getInstance("SHA1withDSA", "SUN");
    sig.initVerify(pubKey);

    sig.update(data, 0, data.length);

    return sig.verify(sigToVerify);
  }

}
