package com.neverpile.eureka.plugin.audit.verification.hashchain;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.neverpile.eureka.plugin.audit.verification.AuditHash;

public class ChainTest {

  @Test
  public void testThat_hashChainLinksAreConnected() {
    HashChain hc = new HashChain();
    HashChainLink hcl0 = new HashChainLink("auditId0", new AuditHash("auditId0".getBytes()));
    HashChainLink hcl1 = new HashChainLink("auditId1", new AuditHash("auditId1".getBytes()));
    HashChainLink hcl2 = new HashChainLink("auditId2", new AuditHash("auditId2".getBytes()));
    hc.addElement(hcl0);
    hc.addElement(hcl1);
    hc.addElement(hcl2);

    assertEquals(hcl0, hc.getHead().getParent().getParent());
  }

  @Test
  public void testThat_hashChainCreatesLinkHashes() {
    HashChain hc = new HashChain();
    HashChainLink hcl0 = new HashChainLink("auditId0", new AuditHash("auditId0".getBytes()));
    HashChainLink hcl1 = new HashChainLink("auditId1", new AuditHash("auditId1".getBytes()));
    HashChainLink hcl2 = new HashChainLink("auditId2", new AuditHash("auditId2".getBytes()));
    HashChainLink hcl3 = new HashChainLink("auditId3", new AuditHash("auditId3".getBytes()));
    hc.addElement(hcl0);
    hc.addElement(hcl1);
    hc.addElement(hcl2);
    hc.addElement(hcl3);

    // @formatter:off
    AuditHash proofHash =
      new AuditHash(
        new AuditHash(
            new AuditHash(
              new AuditHash("auditId0".getBytes(),"auditId0".getBytes()
              ), new AuditHash("auditId1".getBytes())
            ), new AuditHash("auditId2".getBytes())
        ), new AuditHash("auditId3".getBytes())
      );
    // @formatter:on
    assertArrayEquals(proofHash.getHash(), hc.getHead().getLinkHash().getHash());
  }
}
