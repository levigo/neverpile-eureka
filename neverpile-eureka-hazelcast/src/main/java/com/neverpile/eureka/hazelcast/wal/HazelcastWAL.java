package com.neverpile.eureka.hazelcast.wal;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MultiMap;
import com.neverpile.eureka.tracing.TraceInvocation;
import com.neverpile.eureka.tracing.Tag;
import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;
import com.neverpile.eureka.tx.wal.WALException;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.util.ActionEntry;
import com.neverpile.eureka.tx.wal.util.Entry;
import com.neverpile.eureka.tx.wal.util.EventEntry;

public class HazelcastWAL implements WriteAheadLog {

  protected static final Logger logger = LoggerFactory.getLogger(HazelcastWAL.class);

  public static final class TransactionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Date started;

    public int recoveryAttempts;

    public TransactionRecord() {
      super();
      this.started = new Date();
    }

    public int recoveryAttempts() {
      return recoveryAttempts;
    }

    public void setRecoveryAttempts(final int recoveryAttempts) {
      this.recoveryAttempts = recoveryAttempts;
    }

    public Date started() {
      return started;
    }

    public boolean startedBefore(final Date when) {
      return started.before(when);
    }

    public void incrementRecoveryAttempts() {
      recoveryAttempts++;
    }

    @Override
    public String toString() {
      return "TransactionRecord [started=" + started + ", recoveryAttempts=" + recoveryAttempts + "]";
    }
  }

  @Autowired
  private HazelcastInstance hazelcast;

  private IMap<String, TransactionRecord> transactions;

  private MultiMap<String, Entry> entries;

  @Value("${neverpile.wal.hazelcast.auto-rollback-timeout:600}")
  private int autoRollbackTimeout = 600;

  @Value("${neverpile.wal.hazelcast.max-recovery-attempts:10}")
  private final int maxRecoveryAttempts = 10;

  @PostConstruct
  public void start() throws IOException {
    String txMapName = getClass().getName() + "-Transactions";

    transactions = hazelcast.getMap(txMapName);

    String entriesMapName = getClass().getName() + "-Entries";

    entries = hazelcast.getMultiMap(entriesMapName);
  }

  @Scheduled(fixedRateString = "${neverpile.wal.hazelcast.prune-interval:10000}")
  public void pruneTransactions() throws InterruptedException {
    ILock lock = hazelcast.getLock(getClass().getName() + "-TxPrune");
    if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
      try {
        Date rollbackTransactionsStartedBefore = Date.from(
            Instant.now().minus(autoRollbackTimeout, ChronoUnit.SECONDS));

        // find transactions with a completion event...
        transactions.keySet().stream() //
            .filter(id -> queryEntries(id, e -> isCompletedEvent(e)).findAny().isPresent()) //
            .forEach(id -> {
              // ...and clear the corresponding events and transactions
              entries.remove(id);
              transactions.remove(id);
            });

        // find transactions older than the auto rollback timeout without completion events...
        transactions.entrySet().stream() //
            .filter(tx -> tx.getValue().startedBefore(rollbackTransactionsStartedBefore)
                && !queryEntries(tx.getKey(), e -> isCompletedEvent(e)).findAny().isPresent()) //
            .forEach(e -> {
              // ...and try to roll them back.
              String id = e.getKey();
              TransactionRecord tx = e.getValue();

              logger.info("Rolling back incomplete transaction {}, started at {}", id, tx.started());
              tx.incrementRecoveryAttempts();
              try {
                applyLoggedActions(id, ActionType.ROLLBACK, true);
                logCompletion(id);
              } catch (Exception f) {
                if (tx.recoveryAttempts() <= maxRecoveryAttempts) {
                  logger.warn("Rollback of TX {} failed ({}. attempt, will retry)", id, tx.recoveryAttempts(), f);
                  transactions.replace(id, tx);
                } else {
                  logger.error("Rollback of TX {} failed ({}. attempt, will give up)", id, tx.recoveryAttempts(), f);
                  logCompletion(id);
                }
              }
            });

        if (logger.isDebugEnabled()) {
          logger.debug("Transaction housekeeping completed - TX currently pending:");
          transactions.forEach((id, tx) -> {
            logger.debug("  TX {}: {}", id, tx);
            queryEntries(id).forEach(f -> logger.info("    Entry: {}", f));
          });
        }
      } finally {
        lock.unlock();
      }
    }
  }

  private boolean isCompletedEvent(final Entry e) {
    return e instanceof EventEntry && ((EventEntry) e).type == EventType.COMPLETED;
  }

  private void applyActions(final ActionType type, final List<Entry> txEntries) {
    txEntries.forEach(o -> {
      if (o instanceof ActionEntry && ((ActionEntry) o).type == type) {
        ActionEntry ae = (ActionEntry) o;
        try {
          ae.apply();
        } catch (Exception e) {
          throw new WALException("Exception running WAL action of type " + ae.type + " for tx " + ae.txId, e);
        }
      }
    });
  }

  /*
   * (non-Javadoc)
   *
   * @see com.neverpile.eureka.tx.wal.local.WriteAheadLog#logAction(java.lang.String,
   * com.neverpile.eureka.tx.wal.local.FileBasedWAL.ActionType,
   * com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction)
   */
  @Override
  @TraceInvocation
  public void logAction(final String id, @Tag(name="action") final ActionType type, final TransactionalAction action) {
    logger.debug("Logging {} action for tx {}: {}", type, id, action);
    ActionEntry entry = new ActionEntry(id, type, action);
    log(id, entry);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.neverpile.eureka.tx.wal.local.WriteAheadLog#logCompletion(java.lang.String)
   */
  @Override
  @TraceInvocation
  public void logCompletion(final String id) {
    logEvent(id, EventType.COMPLETED);

    // immediately purge tx from cache
    transactions.remove(id);
  }

  private void logEvent(final String id, final EventType eventType) {
    logger.debug("Logging {} event for tx {}", eventType, id);
    log(id, new EventEntry(id, eventType));
  }


  private void log(final String id, final Entry entry) {
    hazelcast.executeTransaction(ctx -> {
      transactions.putIfAbsent(id, new TransactionRecord());
      entries.put(id, entry);
      return null;
    });
  }

  /*
   * (non-Javadoc)
   *
   * @see com.neverpile.eureka.tx.wal.local.WriteAheadLog#applyLoggedActions(java.lang.String,
   * com.neverpile.eureka.tx.wal.local.FileBasedWAL.ActionType, boolean)
   */
  @Override
  public void applyLoggedActions(final String id, final ActionType type, final boolean reverseOrder) {
    List<Entry> txEntries = queryEntries(id).collect(Collectors.toList());

    if (reverseOrder)
      Collections.reverse(txEntries);

    // roll back the actions
    applyActions(type, txEntries);
  }

  private Stream<Entry> queryEntries(final String id) {
    return entries.get(id).stream();
  }

  private Stream<Entry> queryEntries(final String id, final Predicate<Entry> predicate) {
    return entries.get(id).stream().filter(predicate);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.neverpile.eureka.tx.wal.local.WriteAheadLog#sync()
   */
  @Override
  public void sync() {
    // nothng to do
  }

  public int getAutoRollbackTimeout() {
    return autoRollbackTimeout;
  }

  public void setAutoRollbackTimeout(final int autoRollbackTimeout) {
    this.autoRollbackTimeout = autoRollbackTimeout;
  }
}
