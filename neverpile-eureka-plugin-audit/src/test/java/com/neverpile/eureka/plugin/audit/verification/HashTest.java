package com.neverpile.eureka.plugin.audit.verification;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class HashTest {

  @Test
  public void testThat_hashIsStable() {
    AuditHash mh1 = new AuditHash("foo".getBytes());
    AuditHash mh2 = new AuditHash("foo".getBytes());
    assertArrayEquals(mh1.getHash(), mh2.getHash());

    AuditHash mh3 = new AuditHash("bar".getBytes());
    assertFalse(Arrays.equals(mh1.getHash(), mh3.getHash()));
  }

  @Test
  public void testThat_concatHashIsStable() {
    AuditHash mh1 = new AuditHash("foo".getBytes());
    AuditHash mh2 = new AuditHash("foo".getBytes());
    AuditHash mh3 = new AuditHash("bar".getBytes());

    AuditHash mh4 = new AuditHash("foo".getBytes(), "foo".getBytes());
    AuditHash mh5 = new AuditHash(mh1, mh2);
    assertArrayEquals(mh4.getHash(), mh5.getHash());

    AuditHash mh6 = new AuditHash(mh1, mh3);
    assertFalse(Arrays.equals(mh4.getHash(), mh6.getHash()));
  }

  @Test
  public void testThat_hashEqualityCanBeDetermined() {
    AuditHash mh1 = new AuditHash("foo".getBytes());
    AuditHash mh2 = new AuditHash("foo".getBytes());
    assertTrue(mh1.equals(mh2));

    AuditHash mh3 = new AuditHash("bar".getBytes());
    assertFalse(mh1.equals(mh3));
    ;
  }

  @Test
  public void testThat_hashOfNullThrowsException() {
    assertThrows(NullPointerException.class, () -> {
      new AuditHash(null);
    });
  }

  @Test
  public void testThat_concatHashWithFirstArgumentNullThrowsException() {
    assertThrows(NullPointerException.class, () -> {
      new AuditHash(null, "foo".getBytes());
    });
  }

  @Test
  public void testThat_concatHashWithSecondArgumentNullThrowsException() {
    assertThrows(NullPointerException.class, () -> {
      new AuditHash("foo".getBytes(), null);
    });
  }

  @Test
  public void testThat_hashToStringWorks() {
    AuditHash mh1 = new AuditHash("bar".getBytes(StandardCharsets.UTF_8));
    assertEquals("FCDE2B2EDBA56BF408601FB721FE9B5C338D10EE429EA04FAE5511B68FBF8FB9", mh1.toString());
  }
}
