package com.neverpile.eureka.plugin.audit.verification.merkletree;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.neverpile.eureka.plugin.audit.verification.AuditHash;

public class TreeTest {

  @Test
  public void testThat_TreeIsConstructedCorrectly() {
    MerkleTree mt1 = new MerkleTree();

    AuditHash mh1 = new AuditHash("foo".getBytes());
    MerkleNode mnLeaf1 = new MerkleNode(mh1, 0);

    AuditHash mh2 = new AuditHash("bar".getBytes());
    MerkleNode mnLeaf2 = new MerkleNode(mh2, 1);

    AuditHash mh3 = new AuditHash("baz".getBytes());
    MerkleNode mnLeaf3 = new MerkleNode(mh3, 2);

    mt1.addLeaf(mnLeaf1);
    mt1.addLeaf(mnLeaf2);
    mt1.addLeaf(mnLeaf3);

    mt1.buildTreeFromLeaves();

    assertEquals("A7A648FCE0E4F754A6F8F73E75CFFA074FF296D1E8F95877D672FDEEE2A83045",
        mt1.getRootNode().getNodeHash().toString());

    assertEquals(3, mt1.getLeaves().size());

    MerkleTree mt2 = new MerkleTree();

    mt2.buildTreeFromLeaves();

    assertNull(mt2.getRootNode());
  }

  @Test
  public void testThat_treeProofNodesGetGeneratedCorrectly() {
    MerkleTree mt1 = getMerkleTreeWith7Nodes();

    AuditHash mh8 = new AuditHash("bar3".getBytes());
    MerkleNode mnLeaf8 = new MerkleNode(mh8, 7);

    AuditHash mh9 = new AuditHash("bar3".getBytes());
    MerkleNode mnLeaf9 = new MerkleNode(mh9, 8);

    MerkleTree.MerkleTreeVaildator mtv = new MerkleTree.MerkleTreeVaildator();

    mt1.buildTreeFromLeaves();

    assertEquals(3, mt1.getProofNodes().size());

    mt1.addLeaf(mnLeaf8);

    mt1.buildTreeFromLeaves();

    assertEquals(1, mt1.getProofNodes().size());

    mt1.addLeaf(mnLeaf9);

    mt1.buildTreeFromLeaves();

    assertEquals(2, mt1.getProofNodes().size());
  }

  @Test
  public void testThat_updatesCanBeVerifiedViaProofNodes() {
    MerkleTree mt1 = getMerkleTreeWith7Nodes();

    AuditHash mh8 = new AuditHash("bar3".getBytes());
    MerkleNode mnLeaf8 = new MerkleNode(mh8, 7);

    AuditHash mh9 = new AuditHash("bar3".getBytes());
    MerkleNode mnLeaf9 = new MerkleNode(mh9, 8);

    MerkleTree.MerkleTreeVaildator mtv = new MerkleTree.MerkleTreeVaildator();

    mt1.buildTreeFromLeaves();
    List<ProofNode> proofNodesBeforeUpdate = mt1.getProofNodes();

    assertTrue(mtv.validateTreeAgainstProofNodes(mt1, proofNodesBeforeUpdate));

    mt1.addLeaf(mnLeaf8);

    mt1.buildTreeFromLeaves();
    List<ProofNode> proofNodesAfterUpdate = mt1.getProofNodes();

    assertTrue(mtv.validateTreeAgainstProofNodes(mt1, proofNodesAfterUpdate));
    assertTrue(mtv.validateTreeAgainstProofNodes(mt1, proofNodesBeforeUpdate));

    mt1.addLeaf(mnLeaf9);

    mt1.buildTreeFromLeaves();
    proofNodesAfterUpdate = mt1.getProofNodes();

    assertTrue(mtv.validateTreeAgainstProofNodes(mt1, proofNodesAfterUpdate));
    assertTrue(mtv.validateTreeAgainstProofNodes(mt1, proofNodesBeforeUpdate));
  }

  @Test
  public void testThat_treeWithTamperedNodesWillNotGetVerified() {
    MerkleTree mt1 = getMerkleTreeWith7Nodes();

    AuditHash mh8 = new AuditHash("bar3".getBytes());
    MerkleNode mnLeaf8 = new MerkleNode(mh8, 0);

    MerkleTree.MerkleTreeVaildator mtv = new MerkleTree.MerkleTreeVaildator();

    mt1.buildTreeFromLeaves();
    List<ProofNode> proofNodesBeforeTampering = mt1.getProofNodes();

    assertTrue(mtv.validateTreeAgainstProofNodes(mt1, proofNodesBeforeTampering));

    MerkleNode tempNode = mt1.getRootNode();
    while (!tempNode.isLeaf()) {
      tempNode = tempNode.getLeftChild();
    }

    tempNode.getParent().setRightChild(mnLeaf8);

    assertFalse(mtv.validateTreeAgainstProofNodes(mt1, proofNodesBeforeTampering));
  }

  @Test
  public void testThat_TreeFromProofProducesEqualResultsToFullTree(){
    MerkleTree mt1 = getMerkleTreeWith7Nodes();
    MerkleTree mt2 = new MerkleTree();
    mt1.buildTreeFromLeaves();
    List<ProofNode> proof = mt1.getProofNodes();

    AuditHash mh8 = new AuditHash("bar3".getBytes());
    MerkleNode mnLeaf8 = new MerkleNode(mh8, 7);
    AuditHash mh8_2 = new AuditHash("bar3".getBytes());
    MerkleNode mnLeaf8_2 = new MerkleNode(mh8_2, 7);

    mt1.addLeaf(mnLeaf8);
    mt2.addLeaf(mnLeaf8_2);

    mt2.buildTreeWithProof(proof);
    mt1.buildTreeFromLeaves();

    List<ProofNode> finalProof1 = mt1.getProofNodes();
    List<ProofNode> finalProof2 = mt2.getProofNodes();

    assertEquals(finalProof1.size(), finalProof2.size());

    for (int i = 0; i < finalProof1.size(); i++) {
      assertEquals(finalProof1.get(i).getIndex(),finalProof2.get(i).getIndex());
      assertEquals(finalProof1.get(i).getDepth(), finalProof2.get(i).getDepth());
      assertArrayEquals(finalProof1.get(i).getNodeHash().getHash(), finalProof2.get(i).getNodeHash().getHash());
    }
  }


  @Test
  public void testThat_treeToStringWorks() {
    MerkleTree mt = getMerkleTreeWith7Nodes();
    mt.buildTreeFromLeaves();
    assertTrue(mt.toString().contains(mt.getRootNode().getNodeHash().toString()));
  }

  static MerkleTree getMerkleTreeWith7Nodes() {
    MerkleTree mt1 = new MerkleTree();

    AuditHash mh1 = new AuditHash("foo1".getBytes());
    MerkleNode mnLeaf1 = new MerkleNode(mh1, 0);

    AuditHash mh2 = new AuditHash("bar1".getBytes());
    MerkleNode mnLeaf2 = new MerkleNode(mh2, 1);

    AuditHash mh3 = new AuditHash("baz1".getBytes());
    MerkleNode mnLeaf3 = new MerkleNode(mh3, 2);

    AuditHash mh4 = new AuditHash("foo2".getBytes());
    MerkleNode mnLeaf4 = new MerkleNode(mh4, 3);

    AuditHash mh5 = new AuditHash("bar2".getBytes());
    MerkleNode mnLeaf5 = new MerkleNode(mh5, 4);

    AuditHash mh6 = new AuditHash("baz2".getBytes());
    MerkleNode mnLeaf6 = new MerkleNode(mh6, 5);

    AuditHash mh7 = new AuditHash("foo3".getBytes());
    MerkleNode mnLeaf7 = new MerkleNode(mh7, 6);

    mt1.addLeaf(mnLeaf1);
    mt1.addLeaf(mnLeaf2);
    mt1.addLeaf(mnLeaf3);
    mt1.addLeaf(mnLeaf4);
    mt1.addLeaf(mnLeaf5);
    mt1.addLeaf(mnLeaf6);
    mt1.addLeaf(mnLeaf7);
    return mt1;
  }
}
