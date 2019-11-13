package com.neverpile.eureka.tx.wal;

import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;

public interface WriteAheadLog {

  public enum ActionType {
    ROLLBACK, COMMIT
  }

  public enum EventType {
    COMPLETED, ROLLBACK
  }

  void logAction(String id, ActionType type, TransactionalAction action);

  void logCompletion(String id);

  void applyLoggedActions(String id, ActionType type, boolean reverseOrder);

  void sync();

}