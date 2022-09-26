package com.neverpile.eureka.hazelcast.wal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.eureka.hazelcast.wal.HazelcastWAL;
import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;
import com.neverpile.eureka.tx.wal.WriteAheadLog.ActionType;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class, properties = {
    "neverpile.wal.ignite.auto-rollback-timeout=1", "neverpile.wal.ignite.prune-interval=10000"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class HazelcastWALPerformanceTest {
  @Autowired
  HazelcastWAL wal;

  static class AnAction implements TransactionalAction {
    private static final long serialVersionUID = 1L;

    @Override
    public void run() {
      // nothing to do
    }
  }

  @Test
  public void measureOverheadPerCommit() {
    for (int i = 0; i < 100; i++) {
      String id = "warmup" + i;
      wal.logAction(id, ActionType.COMMIT, new AnAction());
      wal.logAction(id, ActionType.ROLLBACK, new AnAction());
      wal.applyLoggedActions(id, ActionType.COMMIT, false);
      wal.logCompletion(id);
    }

    long start = System.currentTimeMillis();
    for (int i = 0; i < 1000; i++) {
      String id = "measure" + i;
      wal.logAction(id, ActionType.COMMIT, new AnAction());
      wal.logAction(id, ActionType.COMMIT, new AnAction());
      wal.logAction(id, ActionType.COMMIT, new AnAction());
      wal.logAction(id, ActionType.COMMIT, new AnAction());
      wal.logAction(id, ActionType.ROLLBACK, new AnAction());
      wal.logAction(id, ActionType.ROLLBACK, new AnAction());
      wal.logAction(id, ActionType.ROLLBACK, new AnAction());
      wal.logAction(id, ActionType.ROLLBACK, new AnAction());
      wal.applyLoggedActions(id, ActionType.COMMIT, false);
      wal.logCompletion(id);
    }
    System.out.println("Overhead per transaction: " + (System.currentTimeMillis() - start) / 1000 + " ms");
  }
}
