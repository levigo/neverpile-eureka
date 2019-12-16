package com.neverpile.eureka.plugin.audit.verification;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class AuditHash implements Serializable {

  private byte[] hash;

  public AuditHash() {
  }

  public AuditHash(byte[] data) {
    this.hash = hashData(data);
  }

  public AuditHash(byte[] data1, byte[] data2) {
    this(concatData(hashData(data1), hashData(data2)));
  }

  public AuditHash(AuditHash data1, AuditHash data2) {
    this(concatData(data1.getHash(), data2.getHash()));
  }

  public AuditHash(Serializable data) {
    this(getBytes(data));
  }

  public AuditHash(Serializable data1, Serializable data2) {
    this(getBytes(data1), getBytes(data2));
  }

  public byte[] getHash() {
    return hash;
  }

  public void setHash(byte[] hash) {
    this.hash = hash;
  }

  public boolean equals(AuditHash node) {
    return null != node && Arrays.equals(this.getHash(), node.getHash());
  }

  @Override
  public String toString() {
    return bytesToHexString(this.hash);
  }

  private static String bytesToHexString(byte[] hash) {
    StringBuilder hexString = new StringBuilder();
    for (int i = 0; i < hash.length; i++) {
      String hex = Integer.toHexString(0xff & hash[i]);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString().toUpperCase();
  }

  private static byte[] hashData(byte[] data) {
    if (null == data) {
      throw new NullPointerException("Data to hash cannot be null.");
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(data);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static byte[] getBytes(Serializable object) {
    if (null == object) {
      throw new NullPointerException("Data to hash cannot be null.");
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(object);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return baos.toByteArray();

  }

  private static byte[] concatData(byte[] data1, byte[] data2) {
    if (null == data1 || null == data2) {
      throw new NullPointerException("Data to hash cannot be null.");
    }
    byte[] concatData = new byte[data1.length + data2.length];
    System.arraycopy(data1, 0, concatData, 0, data1.length);
    System.arraycopy(data2, 0, concatData, data1.length, data2.length);
    return concatData;
  }
}
