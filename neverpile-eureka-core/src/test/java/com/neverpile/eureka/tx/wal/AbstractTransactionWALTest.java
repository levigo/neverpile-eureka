package com.neverpile.eureka.tx.wal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;


public abstract class AbstractTransactionWALTest {

  @Autowired
  TransactionWAL wal;

  @Autowired
  TransactionTemplate transactionTemplate;

  @Test
  @Transactional
  public void testThat_walCanBeAccessed() {
    SomeTransactionalAction action = new SomeTransactionalAction("testThat_walCanBeAccessed");

    wal.appendUndoAction(action);

    action.assertNotExecuted();
  }

  @Test
  public void testThat_walPerformsCommit() {
    SomeTransactionalAction action = new SomeTransactionalAction("testThat_walPerformsCommit");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        wal.appendCommitAction(action);
        return null;
      }
    });

    action.assertExecuted();
  }
  
  @Test
  public void testThat_walPerformsCommitOfMultipleActions() {
    SomeTransactionalAction a1 = new SomeTransactionalAction("testThat_walPerformsCommitOfMultipleActions 1");
    SomeTransactionalAction a2 = new SomeTransactionalAction("testThat_walPerformsCommitOfMultipleActions 2");
    SomeTransactionalAction a3 = new SomeTransactionalAction("testThat_walPerformsCommitOfMultipleActions 3");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        wal.appendCommitAction(a1);
        wal.appendCommitAction(a2);
        wal.appendUndoAction(a3);
        return null;
      }
    });

    a1.assertExecuted();
    a2.assertExecuted();
    a3.assertNotExecuted();
  }
  
  @Test
  public void testThat_walPerformsRollback() {
    SomeTransactionalAction action = new SomeTransactionalAction("testThat_walPerformsRollback");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        wal.appendUndoAction(action);
        status.setRollbackOnly();
        return null;
      }
    });

    action.assertExecuted();
  }

  @Test
  public void testThat_walPerformsRollbackOfMultipleActions() {
    SomeTransactionalAction a1 = new SomeTransactionalAction("testThat_walPerformsRollbackOfMultipleActions 1");
    SomeTransactionalAction a2 = new SomeTransactionalAction("testThat_walPerformsRollbackOfMultipleActions 2");
    SomeTransactionalAction a3 = new SomeTransactionalAction("testThat_walPerformsRollbackOfMultipleActions 3");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        wal.appendUndoAction(a1);
        wal.appendUndoAction(a2);
        wal.appendCommitAction(a2);
        status.setRollbackOnly();
        return null;
      }
    });

    a1.assertExecuted();
    a2.assertExecuted();
    a3.assertNotExecuted();
  }

  @Test
  public void testThat_walPerformsRollbackOFailedTransactionsOnly() {
    SomeTransactionalAction a1 = new SomeTransactionalAction("testThat_walPerformsRollbackOFailedTransactionsOnly 1");
    SomeTransactionalAction a2 = new SomeTransactionalAction("testThat_walPerformsRollbackOFailedTransactionsOnly 2");

    // unsuccessful transaction
    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        wal.appendUndoAction(a1);
        status.setRollbackOnly();
        return null;
      }
    });

    // successful transaction
    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        wal.appendUndoAction(a2);
        return null;
      }
    });

    a1.assertExecuted();
    a2.assertNotExecuted();
  }
}
