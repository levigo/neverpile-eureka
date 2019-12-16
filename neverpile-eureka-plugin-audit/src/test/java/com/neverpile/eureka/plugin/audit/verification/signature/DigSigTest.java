package com.neverpile.eureka.plugin.audit.verification.signature;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.eureka.plugin.audit.verification.DigSigGen;
import com.neverpile.eureka.plugin.audit.verification.DigSigVer;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class DigSigTest {

  @Autowired
  DigSigGen dsg;

  @Autowired
  DigSigVer dsv;

  @Test
  public void testThat_signatureCanBeCreated() throws Exception {
    byte[] sig = dsg.signData("foo".getBytes());
    assertNotNull(sig);
    assertNotEquals(0, sig.length);
    assertNotNull(dsg.getPubKey());
    assertNotEquals(0, dsg.getPubKey().length);
  }

  @Test
  public void testThat_signatureCanBeVerified() throws Exception {
    byte[] sig = dsg.signData("foo".getBytes());

    boolean v = new SimpleDigSigVer().verDigSig(dsg.getPubKey(), sig, "foo".getBytes());
    assertTrue(v);
  }

  @Test
  public void testThat_verificationWithWrongPubKeyFails() throws Exception {
    byte[] sig = dsg.signData("foo".getBytes());

    byte[] tamperKey = new byte[dsg.getPubKey().length];
    System.arraycopy(dsg.getPubKey(), 0, tamperKey, 0, dsg.getPubKey().length);
    // Modify a byte by one
    ++tamperKey[dsg.getPubKey().length - 1];

    boolean v = dsv.verDigSig(tamperKey, sig, "foo".getBytes());
    assertFalse(v);
  }

  @Test
  public void testThat_verificationWithWrongSignatureFails() throws Exception {
    byte[] sig = dsg.signData("foo".getBytes());

    byte[] tamperSig = new byte[sig.length];
    System.arraycopy(sig, 0, tamperSig, 0, sig.length);
    // Modify a byte by one
    ++tamperSig[sig.length - 1];

    boolean v = dsv.verDigSig(dsg.getPubKey(), tamperSig, "foo".getBytes());
    assertFalse(v);
  }

  @Test
  public void testThat_verificationWithWrongDataFails() throws Exception {
    byte[] sig = dsg.signData("foo".getBytes());

    boolean v = dsv.verDigSig(dsg.getPubKey(), sig, "bar".getBytes());
    assertFalse(v);
  }


}

@Configuration
class Config {
  @Bean
  DigSigGen getDigSigGen() throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
    return new SimpleDigSigGen();
  }

  @Bean
  DigSigVer getDigSigVer() {
    return new SimpleDigSigVer();
  }
}
