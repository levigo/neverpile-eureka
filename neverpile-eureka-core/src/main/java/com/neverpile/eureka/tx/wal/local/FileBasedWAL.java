package com.neverpile.eureka.tx.wal.local;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.neverpile.common.opentracing.Tag;
import com.neverpile.common.opentracing.TraceInvocation;
import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;
import com.neverpile.eureka.tx.wal.WALException;
import com.neverpile.eureka.tx.wal.WriteAheadLog;
import com.neverpile.eureka.tx.wal.util.ActionEntry;
import com.neverpile.eureka.tx.wal.util.Entry;
import com.neverpile.eureka.tx.wal.util.EventEntry;


@Component
@Lazy
public class FileBasedWAL implements WriteAheadLog {
  protected static final Logger logger = LoggerFactory.getLogger(FileBasedWAL.class);

  public class AppendingObjectOutputStream extends ObjectOutputStream {
    public AppendingObjectOutputStream(final OutputStream out) throws IOException {
      super(out);
    }

    @Override
    protected void writeStreamHeader() throws IOException {
      // do not write a header, but reset
      reset();
    }
  }

  private static class PositionMaintainingInputStream extends InputStream {
    private final FileInputStream fis;

    private long position;

    private final ByteBuffer buffer;

    public PositionMaintainingInputStream(final FileInputStream fis) {
      this.fis = fis;

      buffer = ByteBuffer.allocate(8192);
      buffer.flip();
    }

    @Override
    public int read() throws IOException {
      if (buffer.remaining() < 1)
        if (!fill())
          return -1;

      return buffer.get();
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      if (buffer.remaining() < 1)
        if (!fill())
          return -1;

      int toFetch = Math.min(len, buffer.remaining());

      buffer.get(b, off, toFetch);

      return toFetch;
    }

    private boolean fill() throws IOException {
      buffer.clear();
      int read = fis.getChannel().read(buffer, position);
      if (read > 0) {
        position += read;
        buffer.flip();
      }
      return read > 0;
    }
  }

  @Value("${neverpile-eureka.wal.directory:./data}")
  String logFileDirectory = "./data";

  @Value("${neverpile-eureka.wal.prune-after-completed-transactions:100}")
  int pruneAfterCompletedTransactions = 100;

  private RandomAccessFile logFile;
  private FileOutputStream outputStream;
  private FileInputStream inputStream;

  private final ReadWriteLock fileLock = new ReentrantReadWriteLock(true);

  private final AtomicInteger completedTxInLog = new AtomicInteger();

  @PostConstruct
  public void start() throws IOException {
    File dir = new File(logFileDirectory);
    dir.mkdirs();
    File wal = new File(dir, "tx.log");

    logFile = new RandomAccessFile(wal, "rw");

    outputStream = new FileOutputStream(logFile.getFD());
    inputStream = new FileInputStream(logFile.getFD());

    rollbackIncompleteTransactions();

    // truncate log
    outputStream.getChannel().truncate(0);
  }

  private void rollbackIncompleteTransactions() {
    try {
      // collect list of completed (either committed or properly rolled back) transactions
      Set<String> completedTxIds = new HashSet<>();
      foreachLogEntry(o -> {
        if (o instanceof EventEntry) {
          EventEntry ee = (EventEntry) o;
          if (ee.type == EventType.COMPLETED) {
            completedTxIds.add(ee.txId);
          }
        }
      });

      // apply all rollback-actions from incomplete transactions
      foreachLogEntryReversed(o -> {
        if (o instanceof ActionEntry) {
          ActionEntry ae = (ActionEntry) o;
          if (!completedTxIds.contains(ae.txId) && ae.type == ActionType.ROLLBACK) {
            try {
              ae.apply();
            } catch (Exception e) {
              logger.error("Exception running WAL action of type " + ae.type + " for tx " + ae.txId, e);
            }
          }
        }
      });

      // apply all commit-actions from completed transactions
      foreachLogEntry(o -> {
        if (o instanceof ActionEntry) {
          ActionEntry ae = (ActionEntry) o;
          if (completedTxIds.contains(ae.txId) && ae.type == ActionType.COMMIT) {
            try {
              ae.apply();
            } catch (Exception e) {
              logger.error("Exception running WAL action of type " + ae.type + " for tx " + ae.txId, e);
            }
          }
        }
      });
    } catch (IOException e) {
      logger.error("Rollback of incomplete transactions failed", e);
    }
  }

  private void foreachLogEntry(final Consumer<Entry> c) throws IOException {
    fileLock.readLock().lock();
    try {
      try (ObjectInputStream ois = new ObjectInputStream(new PositionMaintainingInputStream(inputStream))) {
        while (true) {
          c.accept((Entry) ois.readObject());
        }
      } catch (EOFException e) {
        // expected, as there is no sentiel at the end of the log
      } catch (ClassNotFoundException e) {
        // indicates an action, we no longer know
        logger.error("Can't read event log entry due to missing class", e);
      }
    } finally {
      fileLock.readLock().unlock();
    }
  }

  private void foreachLogEntryReversed(final Consumer<Entry> c) throws IOException {
    List<Entry> actions = new ArrayList<Entry>(100);
    foreachLogEntry(actions::add);
    Collections.reverse(actions);
    actions.forEach(c);
  }

  @PreDestroy
  public void stop() throws IOException {
    if (null != logFile) {
      logFile.close();
      logFile = null;
    }
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
  public void logAction(final String id, @Tag(name = "action") final ActionType type,
      final TransactionalAction action) {
    fileLock.writeLock().lock();
    try {
      seekToEndOfFile();
      logger.debug("Logging {} action for tx {} at {}: {}", type, id, outputStream.getChannel().position(), action);
      writeEntry(new ActionEntry(id, type, action));
    } catch (IOException e) {
      throw new WALException("Can't log action", e);
    } finally {
      fileLock.writeLock().unlock();
    }
  }

  private void writeEntry(final Object entry) throws IOException {
    long p = outputStream.getChannel().position();
    ObjectOutputStream oos = p > 0
        ? new AppendingObjectOutputStream(outputStream)
        : new ObjectOutputStream(outputStream);
    oos.writeObject(entry);
    oos.flush();
  }

  private void seekToEndOfFile() throws IOException {
    outputStream.getChannel().position(logFile.length());
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
    completedTxInLog.incrementAndGet();

    if (completedTxInLog.get() >= pruneAfterCompletedTransactions) {
      pruneLog();
    }
  }

  private void pruneLog() {
    fileLock.writeLock().lock();
    try {
      // collect list of completed (either committed or properly rolled back) transactions
      Set<String> completedTxIds = new HashSet<>();
      foreachLogEntry(o -> {
        if (o instanceof EventEntry) {
          EventEntry ee = (EventEntry) o;
          if (ee.type == EventType.COMPLETED) {
            completedTxIds.add(ee.txId);
          }
        }
      });

      // create temporary log file containing only incomplete entries
      File tmp = new File(logFileDirectory, "tx-tmp.log");
      try (ObjectOutputStream tmpOOS = new ObjectOutputStream(new FileOutputStream(tmp))) {
        foreachLogEntry(o -> {
          try {
            if (!completedTxIds.contains(o.txId))
              tmpOOS.writeObject(o);
          } catch (IOException e) {
            throw new WALException("Can't prune log file", e);
          }
        });
      }

      // copy temp log to active log
      outputStream.getChannel().truncate(0);
      try (FileInputStream tmpS = new FileInputStream(tmp)) {
        StreamUtils.copy(tmpS, outputStream);
      }
      outputStream.flush();
      outputStream.getChannel().force(true);

      tmp.delete();
    } catch (IOException e) {
      logger.error("Rollback of incomplete transactions failed", e);
    } finally {
      fileLock.writeLock().unlock();
    }
  }

  private void logEvent(final String id, final EventType eventType) {
    fileLock.writeLock().lock();
    try {
      seekToEndOfFile();
      logger.debug("Logging {} event for tx {} at {}", eventType, id, outputStream.getChannel().position());
      writeEntry(new EventEntry(id, eventType));
    } catch (IOException e) {
      throw new WALException("Can't log action", e);
    } finally {
      fileLock.writeLock().unlock();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.neverpile.eureka.tx.wal.local.WriteAheadLog#applyLoggedActions(java.lang.String,
   * com.neverpile.eureka.tx.wal.local.FileBasedWAL.ActionType, boolean)
   */
  @Override
  public void applyLoggedActions(final String id, final ActionType type, final boolean reverseOrder) {
    try {
      Consumer<Entry> consumer = new Consumer<Entry>() {
        @Override
        public void accept(final Entry o) {
          if (o instanceof ActionEntry) {
            ActionEntry ae = (ActionEntry) o;
            if (Objects.equals(ae.txId, id) && ae.type == type) {
              logger.debug("Applying {} action for tx {}: {}", type, id, ae.action);
              try {
                ae.apply();
              } catch (Exception e) {
                logger.error("Exception running WAL action of type " + ae.type + " for tx " + ae.txId, e);
              }
            }
          }
        }
      };

      if (reverseOrder)
        foreachLogEntryReversed(consumer);
      else
        foreachLogEntry(consumer);
    } catch (EOFException e) {
      // expected, as there is no sentiel at the end of the log
    } catch (IOException e) {
      throw new WALException("Can't apply logged actions", e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.neverpile.eureka.tx.wal.local.WriteAheadLog#sync()
   */
  @Override
  public void sync() {
    try {
      outputStream.getChannel().force(true);
    } catch (IOException e) {
      throw new WALException("Cannot flush WAL", e);
    }
  }
}
