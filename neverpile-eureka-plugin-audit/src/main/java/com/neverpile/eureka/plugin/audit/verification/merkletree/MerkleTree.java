package com.neverpile.eureka.plugin.audit.verification.merkletree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MerkleTree {

  private MerkleNode rootNode = null;

  private List<MerkleNode> leaves;

  private List<ProofNode> proofNodes;

  public MerkleTree() {
    this.leaves = new ArrayList<>();
  }

  public MerkleNode getRootNode() {
    return rootNode;
  }

  public List<MerkleNode> getLeaves() {
    return leaves;
  }

  public List<ProofNode> getProofNodes() {
    return proofNodes;
  }

  public void addLeaf(MerkleNode leaf) {
    this.leaves.add(leaf);
  }

  public void setLeaves(List<MerkleNode> leaves) {
    this.leaves = leaves;
  }

  public void buildTreeFromLeaves() {
    sortLeaves();
    proofNodes = new ArrayList<>();
    this.buildTreeRecursive(this.leaves, 0, Collections.emptyList());
  }

  public void buildTreeWithProof(List<ProofNode> proof) {
    sortProof(proof);
    sortLeaves();
    proofNodes = new ArrayList<>();
    this.buildTreeRecursive(this.leaves, 0, proof);

  }

  private void buildTreeRecursive(List<MerkleNode> nodes, int depth, List<ProofNode> proof) {
    // get ProofNode if present in proof (there should only bo one proof node per depth).
    ProofNode depthProofNode = proof.stream().filter(node -> depth == node.getDepth()).findFirst().orElse(null);
    if (null != depthProofNode) {
      nodes.add(0, new MerkleNode(depthProofNode));
    }
    if (nodes.size() == 1) {
      this.rootNode = nodes.get(0);
      if (null != this.rootNode.getRightChild() && !this.rootNode.getRightChild().isUnstable()) {
        proofNodes.add(new ProofNode(this.rootNode));
      }
    } else if (!nodes.isEmpty()) {
      int nextDepth = depth + 1;
      List<MerkleNode> parents = new ArrayList<>();
      for (int i = 0; i < nodes.size(); i += 2) {
        MerkleNode leftChild = nodes.get(i);
        MerkleNode rightChild = (i + 1 < nodes.size()) ? nodes.get(i + 1) : null;
        MerkleNode parent = new MerkleNode(leftChild, rightChild, nextDepth);
        if (rightChild == null || rightChild.isUnstable()) {
          if (!leftChild.isUnstable()) {
            proofNodes.add(new ProofNode(leftChild));
          }
          parent.setUnstable(true);
        }
        parents.add(parent);
      }
      buildTreeRecursive(parents, nextDepth, proof);
    }
  }

  @Override
  public String toString() {
    return printNodeRecursive(rootNode);
  }

  private String printNodeRecursive(MerkleNode mn) {
    String s = mn.toString();
    if (null != mn.getLeftChild()) {
      s += printNodeRecursive(mn.getLeftChild());
    }
    if (null != mn.getRightChild()) {
      s += printNodeRecursive(mn.getRightChild());
    }
    return s;
  }


  public static class MerkleTreeVaildator {
    public boolean validateTreeAgainstProofNodes(MerkleTree tree, List<ProofNode> proofNodes) {
      boolean proofValid = true;
      for (ProofNode proofNode : proofNodes) {
        MerkleNode tempNode = tree.getRootNode();
        while (proofValid && null != tempNode) {
          if (proofNode.getDepth() >= tempNode.getDepth()) {
            proofValid = proofNode.getNodeHash().equals(tempNode.getNodeHash());
            break;
          }
          if (tempNode.isLeaf()) {
            proofValid = false;
            break;
          } else if (null != tempNode.getRightChild() && tempNode.getRightChild().getIndex() <= proofNode.getIndex()) {
            tempNode = tempNode.getRightChild();
          } else {
            tempNode = tempNode.getLeftChild();
          }
        }
      }
      return proofValid;
    }
  }

  private void sortLeaves() {
    // sort by index, hash value
    this.leaves.sort((o1, o2) -> {
      long cmp = o1.getIndex() - o2.getIndex();
      if (0 == cmp) {
        // if index is equal compare hashes
        cmp = o1.getNodeHash().toString().compareTo(o2.getNodeHash().toString());
        if (0 == cmp) { // cannot build tree with duplicate nodes (same index and hash).
          throw new RuntimeException("Duplicate leaf in tree");
        } else if (0 < cmp) {
          o2.incrementIndex(); // increment index of higher element to prevent collision.
        } else {
          o1.incrementIndex(); // increment index of higher element to prevent collision.
        }
      }
      return (int) cmp;
    });
  }

  private void sortProof(List<ProofNode> proof) {
    // sort by depth, index
    proof.sort((o1, o2) -> {
      long cmp = o1.getDepth() - o2.getDepth();
      if (0 == cmp) {
        // if index is equal compare hashes
        cmp = o1.getIndex() - o2.getIndex();
        if (0 == cmp) { // cannot build tree with duplicate nodes (same depth and index).
          throw new RuntimeException("Duplicate leaf in proof");
        }
      }
      return (int) cmp;
    });
  }
}
