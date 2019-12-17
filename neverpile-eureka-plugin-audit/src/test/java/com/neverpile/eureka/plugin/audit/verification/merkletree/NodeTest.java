package com.neverpile.eureka.plugin.audit.verification.merkletree;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.neverpile.eureka.plugin.audit.verification.AuditHash;

public class NodeTest {

  @Test
  public void testThat_nodeKnowsItsPositionAsLeaf() {


    AuditHash mh = new AuditHash("foo".getBytes());
    MerkleNode mnLeaf = new MerkleNode(mh, 0);

    new MerkleNode(mnLeaf, null, 0);

    assertTrue(mnLeaf.isLeaf());
    assertFalse(mnLeaf.isRoot());
  }

  @Test
  public void testThat_nodeKnowsItsPositionAsRoot() {
    AuditHash mh = new AuditHash("foo".getBytes());
    MerkleNode mnLeaf = new MerkleNode(mh, 0);

    MerkleNode mnRoot = new MerkleNode(mnLeaf, null, 0);

    assertFalse(mnRoot.isLeaf());
    assertTrue(mnRoot.isRoot());
  }

  @Test
  public void testThat_hashOfNonLeafNodesGetsCalculated() {
    AuditHash mh1 = new AuditHash("foo".getBytes());
    MerkleNode mnLeaf1 = new MerkleNode(mh1, 0);

    AuditHash mh2 = new AuditHash("bar".getBytes());
    MerkleNode mnLeaf2 = new MerkleNode(mh2, 1);

    MerkleNode mnRoot = new MerkleNode(mnLeaf1, mnLeaf2, 0);

    assertArrayEquals(mnRoot.getNodeHash().getHash(), new AuditHash(mh1, mh2).getHash());
  }

  @Test
  public void testThat_hashUpdateGetsPropagatedToParents() {
    AuditHash mh1 = new AuditHash("foo".getBytes());
    MerkleNode mnLeaf1 = new MerkleNode(mh1, 0);

    AuditHash mh2 = new AuditHash("bar".getBytes());
    MerkleNode mnLeaf2 = new MerkleNode(mh2, 1);

    AuditHash mh3 = new AuditHash("baz".getBytes());
    MerkleNode mnLeaf3 = new MerkleNode(mh3, 2);

    MerkleNode mnIntermediate = new MerkleNode(mnLeaf2, null, 1);

    MerkleNode mnRoot = new MerkleNode(mnIntermediate, mnLeaf1, 0);

    AuditHash oldHash = mnRoot.getNodeHash();

    mnIntermediate.setRightChild(mnLeaf3);

    assertFalse(Arrays.equals(oldHash.getHash(), mnRoot.getNodeHash().getHash()));
  }

  @Test
  public void testThat_rightChildCanBaAdded() {
    AuditHash mh1 = new AuditHash("foo".getBytes());
    MerkleNode mnLeaf1 = new MerkleNode(mh1, 0);

    AuditHash mh2 = new AuditHash("bar".getBytes());
    MerkleNode mnLeaf2 = new MerkleNode(mh2, 1);


    MerkleNode mnTest = new MerkleNode(mnLeaf1, null, 0);

    // Set right child.
    AuditHash oldHash = mnTest.getNodeHash();

    mnTest.setRightChild(mnLeaf2);

    assertEquals(mnLeaf2, mnTest.getRightChild());
    assertFalse(Arrays.equals(oldHash.getHash(), mnTest.getNodeHash().getHash()));
  }

  @Test(expected = IllegalStateException.class)
  public void testThat_constructionWithoutLeftChildThrowsException() {
    AuditHash mh = new AuditHash("foo".getBytes());
    MerkleNode mnLeaf = new MerkleNode(mh, 0);

    new MerkleNode(null, mnLeaf, 0);
  }

  @Test(expected = IllegalStateException.class)
  public void testThat_settingRightChildBeforeLeftThrowsException() {
    AuditHash mh = new AuditHash("foo".getBytes());
    MerkleNode mnLeaf = new MerkleNode(mh, 0);

    MerkleNode testNode = new MerkleNode(mh, 0);

    testNode.setRightChild(mnLeaf);
  }

  @Test
  public void testThat_nodeToStringWorks() {
    AuditHash mh = new AuditHash("foo".getBytes());
    MerkleNode mnLeaf = new MerkleNode(mh, 0);

    MerkleNode mnRoot = new MerkleNode(mnLeaf, null, 0);

    assertTrue(mnRoot.toString().contains("63D191440DC78A4ACA5E3AA484F9ADFC71FB3BCE46D7E01BBA91D86A91A0449F"));
  }
}
