package com.neverpile.eureka.plugin.audit.verification.merkletree;

import java.io.Serializable;

import com.neverpile.eureka.plugin.audit.verification.AuditHash;

public class ProofNode implements Serializable {

  private AuditHash nodeHash = null;

  private int depth;
  private long index;

  public ProofNode() {

  }

  public ProofNode(MerkleNode node) {
    this.depth = node.getDepth();
    this.index = node.getIndex();
    this.nodeHash = node.getNodeHash();
  }

  public AuditHash getNodeHash() {
    return nodeHash;
  }

  public void setNodeHash(AuditHash nodeHash) {
    this.nodeHash = nodeHash;
  }

  public int getDepth() {
    return depth;
  }

  public void setDepth(int depth) {
    this.depth = depth;
  }

  public long getIndex() {
    return index;
  }

  public void setIndex(long index) {
    this.index = index;
  }
}
