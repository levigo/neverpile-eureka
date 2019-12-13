package com.neverpile.eureka.plugin.audit.verification.merkletree;

import java.io.Serializable;
import java.util.List;

public class ProofSet implements Serializable {

  private List<ProofNode> proofNodes;

  private String parentId = null;

  private String auditId = null;

  public ProofSet() {
  }

  public ProofSet(List<ProofNode> proofNodes, String parentId, String auditId) {
    this.proofNodes = proofNodes;
    this.parentId = parentId;
    this.auditId = auditId;
  }

  public List<ProofNode> getProofNodes() {
    return proofNodes;
  }

  public void setProofNodes(List<ProofNode> proofNodes) {
    this.proofNodes = proofNodes;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public String getAuditId() {
    return auditId;
  }

  public void setAuditId(String auditId) {
    this.auditId = auditId;
  }
}
