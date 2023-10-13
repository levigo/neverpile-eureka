package com.neverpile.eureka.ignite.wal;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import com.neverpile.eureka.tx.wal.AbstractTimeoutBasedTransactionWALTest;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class, properties = {
    "neverpile.wal.ignite.auto-rollback-timeout=1", "neverpile.wal.ignite.prune-interval=1000",
    "neverpile.wal.ignite.max-recovery-attempts=2"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IgniteTransactionWALTest extends AbstractTimeoutBasedTransactionWALTest {

  @Autowired
  TransactionTemplate transactionTemplate;
}
