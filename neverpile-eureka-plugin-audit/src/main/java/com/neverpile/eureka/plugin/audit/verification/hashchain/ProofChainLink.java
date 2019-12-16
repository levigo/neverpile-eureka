package com.neverpile.eureka.plugin.audit.verification.hashchain;

import java.io.Serializable;

import com.neverpile.eureka.plugin.audit.verification.AuditHash;

public class ProofChainLink implements Serializable {

  private String parentId = null;

  private AuditHash linkHash = null;

  private String auditId = null;

  private byte[] signature = null;

  public ProofChainLink() {
  }

  public ProofChainLink(HashChainLink node) {
    this.auditId = node.getAuditId();
    this.linkHash = node.getLinkHash();
    if (null != node.getParent()) {
      this.parentId = node.getParent().getAuditId();
    }
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public AuditHash getLinkHash() {
    return linkHash;
  }

  public void setLinkHash(AuditHash linkHash) {
    this.linkHash = linkHash;
  }

  public String getAuditId() {
    return auditId;
  }

  public void setAuditId(String auditId) {
    this.auditId = auditId;
  }

  public byte[] getSignature() {
    return signature;
  }

  public void setSignature(byte[] signature) {
    this.signature = signature;
  }
}
