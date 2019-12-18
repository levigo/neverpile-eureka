package com.neverpile.eureka.tx.wal;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

import com.neverpile.eureka.tx.wal.WriteAheadLog.ActionType;
import com.neverpile.eureka.tx.wal.local.FileBasedWAL;

public class FileBasedWALTest {
  @Test
  public void testThat_recoveryOfFailedTransactionsOnRestartWorks() throws Exception {
    SomeTransactionalAction a1 = new SomeTransactionalAction("testThat_recoveryOfFailedTransactionsOnRestartWorks tx1");
    SomeTransactionalAction a2 = new SomeTransactionalAction("testThat_recoveryOfFailedTransactionsOnRestartWorks tx2");

    FileBasedWAL wal = new FileBasedWAL();
    wal.start();
    try {
      wal.logAction("tx1", ActionType.ROLLBACK, a1);
      wal.logAction("tx2", ActionType.ROLLBACK, a2);

      // complete tx1, tx2 remains incomplete
      wal.logCompletion("tx1");

    } finally {
      wal.stop();
    }

    FileBasedWAL recoveryWal = new FileBasedWAL();
    recoveryWal.start();
    recoveryWal.stop();

    a1.assertNotExecuted();
    a2.assertExecuted();
  }

  @Test
  public void testThat_logFileIsPruned() throws Exception {
    FileBasedWAL wal = new FileBasedWAL();
    wal.start();
    try {
      for (int i = 0; i <= 98; i++)
        wal.logCompletion("tx1");

      long length = new File("./data/tx.log").length();

      // trigger pruning
      wal.logCompletion("tx1");
      wal.logCompletion("tx1");
      
      // verify that file has shrunk
      assertThat(new File("./data/tx.log").length(), lessThan(length));
    } finally {
      wal.stop();
    }
  }
  
  @Test
  public void testThat_pruningPreservesActiveEntries() throws Exception {
    SomeTransactionalAction a1 = new SomeTransactionalAction("testThat_recoveryOfFailedTransactionsOnRestartWorks tx1");
    
    FileBasedWAL wal = new FileBasedWAL();
    wal.start();
    
    wal.logAction("tx1", ActionType.ROLLBACK, a1);
    
    try {
      // trigger pruning
      for (int i = 0; i <= 120; i++)
        wal.logCompletion("txXXX");

      wal.applyLoggedActions("tx1", ActionType.ROLLBACK, false);
      
      a1.assertExecuted();
    } finally {
      wal.stop();
    }
    
  }
}
