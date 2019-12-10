package com.neverpile.eureka.tx.wal.local;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.annotation.RequestScope;

import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.WriteAheadLog.ActionType;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

@Component
@RequestScope
@ConditionalOnMissingBean(TransactionWAL.class)
public class DefaultTransactionWAL implements TransactionWAL {
  private static final Logger logger = LoggerFactory.getLogger(DefaultTransactionWAL.class);

  private String id = UUID.randomUUID().toString();

  @Autowired
  WriteAheadLog wal;

  @Autowired
  Tracer tracer;

  private final TransactionSynchronizationAdapter synchronization = new TransactionSynchronizationAdapter() {
    @Override
    public void afterCompletion(final int status) {
      Span span = tracer //
          .buildSpan("tx-wal.after-completion") //
          .withTag("status", status == TransactionSynchronizationAdapter.STATUS_COMMITTED ? "committed" : "rollback") //
          .withTag("wal-implementation", wal.getClass().getSimpleName()) //
          .start();
      try (Scope scope = tracer.activateSpan(span)) {
        if (status == TransactionSynchronizationAdapter.STATUS_COMMITTED)
          commit();
        else if (status == TransactionSynchronizationAdapter.STATUS_ROLLED_BACK)
          rollback();
        else {
          logger.error("Unsupported afterCompletion status: {} - performing rollback", status);
          rollback();
        }
      } finally {
        // we need to use a new ID after each TX completion
        id = UUID.randomUUID().toString();
        span.finish();
      }
    }

    @Override
    public void beforeCommit(final boolean readOnly) {
      wal.sync();
    }
  };

  private void ensureRegistration() {
    // Synchronizations can be registered multiple times as they are maintained in a set.
    // Additionally, synchronization must be re-registered after a commit.
    TransactionSynchronizationManager.registerSynchronization(synchronization);
  }

  @Override
  public void appendUndoAction(final TransactionalAction action) {
    ensureRegistration();

    logAction(id, ActionType.ROLLBACK, action);
  }

  @Override
  public void appendCommitAction(final TransactionalAction action) {
    ensureRegistration();

    logAction(id, ActionType.COMMIT, action);
  }

  protected void logAction(final String id, final ActionType type, final TransactionalAction action) {
    Span span = tracer //
        .buildSpan("tx-wal.log-action") //
        .withTag("action", type.name()) //
        .withTag("wal-implementation", wal.getClass().getSimpleName()) //
        .start();
    try (Scope scope = tracer.activateSpan(span)) {
      wal.logAction(id, type, action);
    } finally {
      span.finish();
    }
  }

  protected void rollback() {
    wal.applyLoggedActions(id, ActionType.ROLLBACK, true);

    wal.logCompletion(id);
  }

  protected void commit() {
    wal.applyLoggedActions(id, ActionType.COMMIT, false);

    wal.logCompletion(id);
  }
}
