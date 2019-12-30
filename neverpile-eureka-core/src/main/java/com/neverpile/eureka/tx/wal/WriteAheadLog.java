package com.neverpile.eureka.tx.wal;

import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;

/**
 * Generic WAL (write ahead log). This class handles logging and applying actions.
 * These Actions ares stored with some sort of persistence or distribution to ensure fault tolerance.
 */
public interface WriteAheadLog {

  /**
   * Action type to specify an use case of a given action. Used in conjunction with
   * {@link com.neverpile.eureka.tx.wal.util.ActionEntry}.
   */
  public enum ActionType {
    ROLLBACK, COMMIT
  }

  /**
   * Event type to mark an Action as complete. Used in conjunction with
   * {@link com.neverpile.eureka.tx.wal.util.EventEntry}.
   */
  public enum EventType {
    COMPLETED, ROLLBACK
  }

  /**
   * Append a new action to the log. Tis action cab be identified by its ID in combination with a {@link ActionType}.
   * The the given {@link TransactionalAction} will be executed when called via {@link WriteAheadLog#applyLoggedActions}
   * When multiple Actions for the same id with different types are appended, only one type can be executed.
   *
   * @param id Action identifier.
   * @param type ActionType depending on the Actions use case.
   * @param action Runnable action to be executed if needed
   */
  void logAction(String id, ActionType type, TransactionalAction action);

  /**
   * This method marks a logged Action as completed. All previous logged action for the given ID are seen as completed.
   *
   * @param id identifier for logged actions to mark as completed.
   */
  void logCompletion(String id);

  /**
   * Executes a logged Action with the specified ID and Type. If multiple Actions with this exact ID and Type where
   * logged the actions will be executed sequentially.
   *
   * @param id Action identifier.
   * @param type Applied ActionType.
   * @param reverseOrder Defines if the execution order should be reversed.
   */
  void applyLoggedActions(String id, ActionType type, boolean reverseOrder);

  /**
   * Force synchronizes the log with distributed instances and/or forces synchronisation with persistence medium.
   */
  void sync();

}