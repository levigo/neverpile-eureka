package com.neverpile.eureka.tx.wal;

import java.io.Serializable;

/**
 * Transaction WAL (write ahead log). This WAL ensures transactional security be definging undo and commit actions in
 * case of an unexpected shutdown or similar. {@link TransactionalAction}s can be registered on the WAL and ensure that
 * ater each transaction, sucessful or not, the Data model is in a consistant state.
 */
public interface TransactionWAL {

  /**
   * Defines an action executed by the Fal on success or failure states.
   * This Action can be executed by the WAL to e.g. restore/retry/undo/complete a transaction.
   */
  public interface TransactionalAction extends Serializable, Runnable {
    
  }

  /**
   * Append an Undo Action to be executed on an failure state.
   * This reverts all transaction changes to a consistant state before the failed transaction occured.
   *
   * @param action action to be executed.
   */
  public void appendUndoAction(TransactionalAction action);

  /**
   * Append commit Action to be ececuted on an successful transaction.
   * Used to clean up after an transcation to leave a consistant state without sideeffects.
   *
   * @param action action to be executed.
   */
  public void appendCommitAction(TransactionalAction action);
  
}
