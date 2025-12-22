package com.neverpile.eureka.plugin.audit.verification;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.Stack;
import java.util.UUID;

import com.neverpile.eureka.model.EncryptionType;

import org.junit.jupiter.api.Test;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.verification.signature.SimpleDigSigGen;
import com.neverpile.eureka.plugin.audit.verification.signature.SimpleDigSigVer;

public class TimingIT {


  @Test
  public void testSignatureOfAuditEvents()
      throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {

    Stack<byte[]> testData = new Stack<>();
    SimpleDigSigGen simpleDigSigGen = new SimpleDigSigGen();
    Random rnd = new Random();
    for (int i = 0; i < 10/*0000*/; i++) {
      testData.push(getRandomAuditEvent(rnd));
    }
    // test signature:
    Instant start = Instant.now();
    for (int i = 0; i < 10/*0000*/; i++) {
      simpleDigSigGen.signData(testData.pop());
    }
    Instant end = Instant.now();
    Duration duration = Duration.between(start, end).dividedBy(100000);
    System.out.println(
        duration.getSeconds() + "s " + duration.getNano() + "ns -> " + ((duration.getSeconds() * 1000.0f) + (
            duration.getNano() / 1000000.0f)) + "ms");
  }

  @Test
  public void testVerifySignatureOfAuditEvents()
      throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException,
      InvalidKeySpecException {

    Stack<byte[]> testData = new Stack<>();
    Stack<byte[]> signatures = new Stack<>();
    SimpleDigSigGen simpleDigSigGen = new SimpleDigSigGen();
    SimpleDigSigVer simpleDigSigVer = new SimpleDigSigVer();
    Random rnd = new Random();
    for (int i = 0; i < 10/*0000*/; i++) {
      byte[] data = getRandomAuditEvent(rnd);
      testData.push(data);
      signatures.push(simpleDigSigGen.signData(data));
    }
    // test verification:
    byte[] key = simpleDigSigGen.getPubKey();
    Instant start = Instant.now();
    for (int i = 0; i < 10/*0000*/; i++) {
      simpleDigSigVer.verDigSig(key, signatures.pop(), testData.pop());
    }
    Instant end = Instant.now();

    Duration duration = Duration.between(start, end).dividedBy(100000);
    System.out.println(
        duration.getSeconds() + "s " + duration.getNano() + "ns -> " + ((duration.getSeconds() * 1000.0f) + (
            duration.getNano() / 1000000.0f)) + "ms");
  }

  private byte[] getRandomAuditEvent(Random rnd) {
    AuditEvent ae = new AuditEvent();
    ae.setTimestamp(Instant.ofEpochMilli(rnd.nextInt()));
    ae.setAuditId(UUID.randomUUID().toString());
    ae.setType(AuditEvent.Type.values()[rnd.nextInt(3)]);
    ae.setDescription("this is a placeholder description text.");
    ae.setEncryption(EncryptionType.values()[rnd.nextInt(2)]);
    ae.setRequestPath("Post: /this/is/an/example/path");
    return getBytes(ae);
  }

  private byte[] getBytes(Serializable object) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(object);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return baos.toByteArray();

  }
}
