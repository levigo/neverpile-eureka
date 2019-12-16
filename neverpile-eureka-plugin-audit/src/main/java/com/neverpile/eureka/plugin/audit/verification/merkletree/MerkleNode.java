package com.neverpile.eureka.plugin.audit.verification.merkletree;

import com.neverpile.eureka.plugin.audit.verification.AuditHash;

public class MerkleNode {

  private AuditHash nodeHash = null;

  private MerkleNode leftChild = null;
  private MerkleNode rightChild = null;
  private MerkleNode parent = null;

  private boolean isUnstable;
  private int depth;
  private long index;

  /**
   * Leaf node constructor.
   *
   * @param hash leaf object hash
   * @param index leaf index in tree
   */
  public MerkleNode(AuditHash hash, long index) {
    this.depth = 0;
    this.index = index;
    this.nodeHash = hash;
  }

  /**
   * Non leaf node Constructor.
   *
   * @param leftChild  may not be null
   * @param rightChild my be null
   * @param depth in tree (0 is root)
   */
  MerkleNode(MerkleNode leftChild, MerkleNode rightChild, int depth) {
    if (leftChild == null) {
      throw new IllegalStateException("Left child node cannot be null on non leaf node.");
    }
    this.depth = depth;
    this.index = leftChild.index;
    this.leftChild = leftChild;
    this.leftChild.parent = this;

    if (rightChild != null) {
      this.rightChild = rightChild;
      this.rightChild.parent = this;
    }

    this.updateHash();
  }

  public MerkleNode(ProofNode node) {
    this.depth = node.getDepth();
    this.index = node.getIndex();
    this.nodeHash = node.getNodeHash();
    this.isUnstable = false;
    this.leftChild = null;
    this.rightChild = null;
    this.parent = null;
  }

  public AuditHash getNodeHash() {
    return nodeHash;
  }

  public MerkleNode getLeftChild() {
    return leftChild;
  }

  public MerkleNode getRightChild() {
    return rightChild;
  }

  public MerkleNode getParent() {
    return parent;
  }

  public boolean isUnstable() {
    return isUnstable;
  }

  public int getDepth() {
    return depth;
  }

  public long getIndex() {
    return index;
  }

  public void incrementIndex() {
    ++index;
  }

  public void setUnstable(boolean unstable) {
    isUnstable = unstable;
  }

  public void setRightChild(MerkleNode rightChild) {
    if (this.leftChild == null) {
      throw new IllegalStateException("Cannot set child on leaf node.");
    }
    this.rightChild = rightChild;
    this.rightChild.parent = this;
    updateHash();
  }

  public boolean isLeaf() {
    return this.leftChild == null && this.rightChild == null;
  }

  public boolean isRoot() {
    return this.parent == null;
  }

  private void updateHash() {
    if (isLeaf()) {
      return;
    } else if (this.rightChild == null) {
      this.nodeHash = new AuditHash(this.leftChild.getNodeHash(), this.leftChild.getNodeHash());
    } else {
      this.nodeHash = new AuditHash(this.leftChild.getNodeHash(), this.rightChild.getNodeHash());
    }

    if (parent != null) {
      parent.updateHash();
    }
  }

  @Override
  public String toString() {
    return String.format("{\n\tindex: %d;\n\tdepth: %d;\n\troot: %b;\n\tleaf: %b;\n\thash: %s;\n}", index, depth,
        isRoot(), isLeaf(), nodeHash.toString());
  }
}
