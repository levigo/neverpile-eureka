package com.neverpile.eureka.plugin.audit.verification.hashchain;

import java.util.List;

import com.neverpile.eureka.plugin.audit.verification.AuditHash;

public class HashChain {

  private HashChainLink head = null;

  public HashChain() {
  }

  public void addElement(HashChainLink element) {
    // Set linkHash
    if (null != element.getAuditHash()) {
      if (null == head) {
        element.setLinkHash(new AuditHash(element.getAuditHash(), element.getAuditHash()));
      } else {
        element.setLinkHash(new AuditHash(head.getLinkHash(), element.getAuditHash()));
      }
    } else if (null == element.getLinkHash()) {
      throw new RuntimeException("Cannot add Element. Insufficient information to create LinkHash.");
    }
    element.setParent(head);
    head = element;
  }

  public void setElements(List<HashChainLink> elements) {
    for (HashChainLink element : elements) {
      addElement(element);
    }
  }

  public HashChainLink getHead() {
    return head;
  }

  public void setHead(HashChainLink head) {
    this.head = head;
  }
}
