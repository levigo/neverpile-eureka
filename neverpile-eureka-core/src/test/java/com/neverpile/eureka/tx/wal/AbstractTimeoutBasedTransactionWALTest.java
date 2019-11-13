package com.neverpile.eureka.tx.wal;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;


public abstract class AbstractTimeoutBasedTransactionWALTest extends AbstractTransactionWALTest {

  @Test
  public void testThat_walPerformsRollbackOnTimeout() {
    SomeTransactionalAction a1 = new SomeTransactionalAction("testThat_walPerformsRollbackOnTimeout 1");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        wal.appendUndoAction(a1);

        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        return null;
      }
    });

    a1.assertExecuted();
  }

  private static final class FirstTimeFailingAction extends SomeTransactionalAction {
    private static final long serialVersionUID = 1L;

    // must be static, since the actions are deserialized upon each recovery attempt
    static int attempts;

    private FirstTimeFailingAction(final String name) {
      super(name);
    }

    @Override
    public void run() {
      System.out.println(getClass().getSimpleName() + ".run() called, attempts=" + attempts);
      if (attempts++ < 1)
        throw new RuntimeException("Failing now, but better luck next time...");

      super.run();
    }
  }

  @Test
  public void testThat_walPerformsMultipleRollbackAttempts() {
    FirstTimeFailingAction.attempts = 0;
    FirstTimeFailingAction a1 = new FirstTimeFailingAction("testThat_walPerformsMultipleRollbackAttempts");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        wal.appendUndoAction(a1);

        try {
          Thread.sleep(5000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        return null;
      }
    });

    a1.assertExecuted();
  }

  private static final class AlwaysFailingAction extends SomeTransactionalAction {
    private static final long serialVersionUID = 1L;

    // must be static, since the actions are deserialized upon each recovery attempt
    static int attempts;

    private AlwaysFailingAction(final String name) {
      super(name);
    }

    @Override
    public void run() {
      attempts++;
      throw new RuntimeException("ka-blam!");
    }
  }

  @Test
  public void testThat_walGivesUpAfterTooManyRecoveryFailures() {
    AlwaysFailingAction.attempts = 0;
    AlwaysFailingAction a1 = new AlwaysFailingAction("testThat_walGivesUpAfterTooManyRecoveryFailures");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        wal.appendUndoAction(a1);

        try {
          Thread.sleep(4000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        return null;
      }
    });

    assertThat(AlwaysFailingAction.attempts, equalTo(3));
  }

  @Test
  public void testThat_walDoesNotRollbackCompletedTx() {
    SomeTransactionalAction a1 = new SomeTransactionalAction("testThat_walDoesNotRollbackCompletedTx 1");

    transactionTemplate.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(final TransactionStatus status) {
        wal.appendUndoAction(a1);
        return null;
      }
    });

    // TX completed - allow WAL to run into timeout
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    a1.assertNotExecuted();
  }
}
