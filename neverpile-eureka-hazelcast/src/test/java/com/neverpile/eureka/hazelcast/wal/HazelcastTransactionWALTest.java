package com.neverpile.eureka.hazelcast.wal;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.neverpile.eureka.tx.wal.AbstractTimeoutBasedTransactionWALTest;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class, properties = {
    "neverpile.wal.hazelcast.auto-rollback-timeout=1", "neverpile.wal.hazelcast.prune-interval=1000",
    "neverpile.wal.hazelcast.max-recovery-attempts=2"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class HazelcastTransactionWALTest extends AbstractTimeoutBasedTransactionWALTest {
  
}
