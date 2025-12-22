package com.neverpile.eureka.tx.wal.util;

import java.io.Serial;

import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;
import com.neverpile.eureka.tx.wal.WriteAheadLog.ActionType;

public class ActionEntry extends Entry {
  @Serial
  private static final long serialVersionUID = 1L;

  public ActionType type;

  public TransactionalAction action;

  public ActionEntry(final String txId, final ActionType type, final TransactionalAction action) {
    super(txId);
    this.type = type;
    this.action = action;
  }

  public void apply() {
    action.run();
  }

  @Override
  public String toString() {
    return "On " + type + ": " + action;
  }
}