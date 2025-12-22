package com.neverpile.eureka.hazelcast.wal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serial;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;

class SomeTransactionalAction implements TransactionalAction {
  @Serial
  private static final long serialVersionUID = 1L;

  private static final Set<String> executedActions = new HashSet<>();

  private final String id = UUID.randomUUID().toString();

  private final String name;

  public SomeTransactionalAction(final String name) {
    this.name = name;
  }

  @Override
  public void run() {
    System.out.println("TransactionalAction: " + name);
    executedActions.add(id);
  }

  public void assertExecuted() {
    assertTrue(executedActions.contains(id), "TransactionalAction: " + name + " was not executed (but should have been)");
  }

  public void assertNotExecuted() {
    assertFalse(executedActions.contains(id), "TransactionalAction: " + name + " was executed (but should not have been)");
  }
}