package com.neverpile.eureka.ignite.wal;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.cache.expiry.EternalExpiryPolicy;

import jakarta.annotation.PostConstruct;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteLock;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import com.neverpile.common.opentracing.Tag;
import com.neverpile.common.opentracing.TraceInvocation;
import com.neverpile.eureka.ignite.IgniteConfigurationProperties;
import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;
import com.neverpile.eureka.tx.wal.WALException;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.util.ActionEntry;
import com.neverpile.eureka.tx.wal.util.Entry;
import com.neverpile.eureka.tx.wal.util.EventEntry;

import io.micrometer.core.annotation.Timed;

public class IgniteWAL implements WriteAheadLog {
  protected static final Logger logger = LoggerFactory.getLogger(IgniteWAL.class);

  public static final class EntryKey implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String transactionId;

    public final long sequenceNumber;

    public EntryKey(final String transactionId, final long sequenceNumber) {
      this.transactionId = transactionId;
      this.sequenceNumber = sequenceNumber;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (sequenceNumber ^ (sequenceNumber >>> 32));
      result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      EntryKey other = (EntryKey) obj;
      if (sequenceNumber != other.sequenceNumber)
        return false;
      if (transactionId == null) {
        if (other.transactionId != null)
          return false;
      } else if (!transactionId.equals(other.transactionId))
        return false;
      return true;
    }
  }

  public static final class TransactionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Instant started;

    public int recoveryAttempts;

    public TransactionRecord() {
      super();
      this.started = Instant.now();
    }

    public int recoveryAttempts() {
      return recoveryAttempts;
    }

    public void setRecoveryAttempts(final int recoveryAttempts) {
      this.recoveryAttempts = recoveryAttempts;
    }

    public Instant started() {
      return started;
    }

    public boolean startedBefore(final Instant when) {
      return started.isBefore(when);
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
  private Ignite ignite;

  @Autowired
  private IgniteConfigurationProperties config;

  private IgniteCache<String, TransactionRecord> transactions;

  private IgniteCache<EntryKey, Entry> entries;

  @Value("${neverpile.wal.ignite.auto-rollback-timeout:600}")
  private int autoRollbackTimeout = 600;

  @Value("${neverpile.wal.ignite.max-recovery-attempts:10}")
  private final int maxRecoveryAttempts = 10;

  private final AtomicLong sequenceCounter = new AtomicLong();

  @PostConstruct
  public void start() throws IOException {
    CacheConfiguration<String, TransactionRecord> ccTx = new CacheConfiguration<>(
        getClass().getName() + "-Transactions");
    ccTx.setBackups(1);
    ccTx.setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());
    ccTx.setCacheMode(CacheMode.REPLICATED);

    if (config.getPersistence().isEnabled())
      ccTx.setDataRegionName("persistent");

    transactions = ignite.getOrCreateCache(ccTx);

    CacheConfiguration<EntryKey, Entry> ccEntries = new CacheConfiguration<>(getClass().getName() + "-Entries");
    ccEntries.setBackups(1);
    ccEntries.setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf());

    ccEntries.setCacheMode(CacheMode.REPLICATED);

    if (config.getPersistence().isEnabled())
      ccEntries.setDataRegionName("persistent");

    entries = ignite.getOrCreateCache(ccEntries);
  }

  @Scheduled(fixedRateString = "${neverpile.wal.ignite.prune-interval:10000}")
  public void pruneTransactions() {
    IgniteLock lock = ignite.reentrantLock(getClass().getName() + "-TxPrune", true, false, true);
    if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
      try {
        Instant rollbackTransactionsStartedBefore = Instant.now().minus(autoRollbackTimeout, ChronoUnit.SECONDS);

        // find transactions with a completion event...
        transactions //
            .query(new ScanQuery<String, TransactionRecord>((k, v) -> queryEntries(k,
                e -> e instanceof EventEntry && ((EventEntry) e).type == EventType.COMPLETED).findAny().isPresent())) //
            .forEach(e -> {
              // ...and clear the corresponding events
              String txId = e.getKey();
              entries.query(new ScanQuery<EntryKey, Entry>((k, v) -> txId.equals(k.transactionId))).forEach(
                  f -> entries.removeAsync(f.getKey()));
            });

        // find transactions older than the auto rollback timeout without completion events...
        transactions //
            .query(new ScanQuery<String, TransactionRecord>((k, v) -> v.startedBefore(rollbackTransactionsStartedBefore) //
                && !queryEntries(k, e -> e instanceof EventEntry
                    && ((EventEntry) e).type == EventType.COMPLETED).findAny().isPresent())) //
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
          transactions.forEach(e -> {
            logger.debug("  TX {}: {}", e.getKey(), e.getValue());
            queryEntries(e.getKey()).forEach(f -> logger.info("    Entry: {}", f));
          });
        }
      } finally {
        lock.unlock();
      }
    }
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
  @Timed(description = "log wal action", extraTags = {
      "subsystem", "ignite.wal"
  }, value = "eureka.ignite.wal.log")
  @TraceInvocation
  public void logAction(final String id,  @Tag(name="action") final ActionType type, final TransactionalAction action) {
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
  @Timed(description = "log wal completion", extraTags = {
      "subsystem", "ignite.wal"
  }, value = "eureka.ignite.wal.completion")
  @TraceInvocation
  public void logCompletion(final String id) {
    logEvent(id, EventType.COMPLETED);

    // immediately purge tx from cache
    Transaction tx = ignite.transactions().txStart();
    try {
      transactions.remove(id);
    } finally {
      tx.commit();
    }
  }


  private void logEvent(final String id, final EventType eventType) {
    logger.debug("Logging {} event for tx {}", eventType, id);
    log(id, new EventEntry(id, eventType));
  }


  private void log(final String id, final Entry entry) {
    Transaction tx = ignite.transactions().txStart();
    try {
      transactions.putIfAbsent(id, new TransactionRecord());
      entries.put(new EntryKey(id, sequenceCounter.getAndIncrement()), entry);
    } finally {
      tx.commit();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see com.neverpile.eureka.tx.wal.local.WriteAheadLog#applyLoggedActions(java.lang.String,
   * com.neverpile.eureka.tx.wal.local.FileBasedWAL.ActionType, boolean)
   */
  @Override
  @Timed(description = "apply wal actions", extraTags = {
      "subsystem", "ignite.wal"
  }, value = "eureka.ignite.wal.apply")
  public void applyLoggedActions(final String id, final ActionType type, final boolean reverseOrder) {
    List<Entry> txEntries = queryEntries(id).collect(Collectors.toList());

    if (reverseOrder)
      Collections.reverse(txEntries);

    // roll back the actions
    applyActions(type, txEntries);
  }

  private Stream<Entry> queryEntries(final String id) {
    return entries //
        .query(new ScanQuery<EntryKey, Entry>((k, v) -> id.equals(k.transactionId))) //
        .getAll().stream() //
        .sorted(Comparator.comparingLong(e -> e.getKey().sequenceNumber)) //
        .map(e -> e.getValue());
  }

  private <E extends Entry> Stream<E> queryEntries(final String id, final Predicate<E> predicate) {
    return entries //
        .query(new ScanQuery<EntryKey, E>((k, v) -> id.equals(k.transactionId) && predicate.test(v))) //
        .getAll().stream() //
        .sorted(Comparator.comparingLong(e -> e.getKey().sequenceNumber)) //
        .map(e -> e.getValue());
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
