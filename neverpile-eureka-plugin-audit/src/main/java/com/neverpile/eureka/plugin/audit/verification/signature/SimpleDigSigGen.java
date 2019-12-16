package com.neverpile.eureka.plugin.audit.verification.signature;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.neverpile.eureka.plugin.audit.verification.DigSigGen;

@Service
public class SimpleDigSigGen implements DigSigGen {

  @Value("${neverpile-eureka.audit.publickey}")
  private String publicKeyEncoded;
  @Value("${neverpile-eureka.audit.privatekey}")
  private String privateKeyEncoded;

  private PublicKey pub;
  private PrivateKey priv;

  private Signature dsa;


  public SimpleDigSigGen() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException {
    if (null != publicKeyEncoded && null != privateKeyEncoded) {
      pub = getPubKey(publicKeyEncoded);
      priv = getPrivKey(privateKeyEncoded);
    }
    if (null == pub || null == priv) {
      KeyPair pair = getKeyPair();

      priv = pair.getPrivate();
      pub = pair.getPublic();
    }

    dsa = getSignature(priv);
  }

  @Override
  public byte[] signData(byte[] data) throws SignatureException {
    dsa.update(data, 0, data.length);
    return dsa.sign();
  }

  @Override
  public byte[] getPubKey() {
    return pub.getEncoded();
  }

  private Signature getSignature(PrivateKey priv)
      throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {
    dsa = Signature.getInstance("SHA1withDSA", "SUN");
    dsa.initSign(priv);
    return dsa;
  }

  private KeyPair getKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
    SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
    keyGen.initialize(1024, random);
    return keyGen.generateKeyPair();
  }

  private PublicKey getPubKey(String key) {
    try {
      byte[] byteKey = key.getBytes();
      X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
      KeyFactory kf = KeyFactory.getInstance("DSA", "SUN");

      return kf.generatePublic(X509publicKey);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private PrivateKey getPrivKey(String key) {
    try {
      byte[] byteKey = key.getBytes();
      X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
      KeyFactory kf = KeyFactory.getInstance("DSA", "SUN");

      return kf.generatePrivate(X509publicKey);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


}
