package com.neverpile.eureka.tx.wal.util;

import java.io.Serializable;

public abstract class Entry implements Serializable {
  private static final long serialVersionUID = 1L;

  public String txId;

  public Entry(final String txId) {
    this.txId = txId;
  }
}