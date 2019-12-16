package com.neverpile.eureka.plugin.audit.verification.hashchain;

import com.neverpile.eureka.plugin.audit.verification.AuditHash;

public class HashChainLink {

  private HashChainLink parent;

  private AuditHash auditHash = null;

  private AuditHash linkHash = null;

  private String auditId = null;

  public HashChainLink(String auditId, AuditHash auditHash) {
    this.auditHash = auditHash;
    this.auditId = auditId;
  }

  public void setParent(HashChainLink parent) {
    this.parent = parent;
  }

  public HashChainLink getParent() {
    return parent;
  }

  public AuditHash getAuditHash() {
    return auditHash;
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
}
