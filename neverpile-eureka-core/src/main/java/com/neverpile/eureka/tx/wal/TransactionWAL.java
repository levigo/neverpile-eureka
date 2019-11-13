package com.neverpile.eureka.tx.wal;

import java.io.Serializable;

public interface TransactionWAL {
  
  public interface TransactionalAction extends Serializable, Runnable {
    
  }
  
  public void appendUndoAction(TransactionalAction action);
  
  public void appendCommitAction(TransactionalAction action);
  
}
