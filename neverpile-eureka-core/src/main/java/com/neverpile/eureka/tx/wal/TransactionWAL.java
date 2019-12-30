package com.neverpile.eureka.tx.wal;

import java.io.Serializable;

/**
 * Transaction WAL (write ahead log). This WAL ensures transactional security be defining undo and commit actions in
 * case of an unexpected shutdown or similar. {@link TransactionalAction}s can be registered on the WAL and ensure that
 * after each transaction, successful or not, the Data model is in a consistent state.
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
   * This reverts all transaction changes to a consistent state before the failed transaction occurred.
   *
   * @param action action to be executed.
   */
  public void appendUndoAction(TransactionalAction action);

  /**
   * Append commit Action to be executed on an successful transaction.
   * Used to clean up after an transaction to leave a consistent state without side effects.
   *
   * @param action action to be executed.
   */
  public void appendCommitAction(TransactionalAction action);
  
}
