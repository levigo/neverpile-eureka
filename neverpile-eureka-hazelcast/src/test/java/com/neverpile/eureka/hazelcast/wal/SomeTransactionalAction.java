package com.neverpile.eureka.hazelcast.wal;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;

class SomeTransactionalAction implements TransactionalAction {
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
    assertTrue("TransactionalAction: " + name + " was not executed (but should have been)", executedActions.contains(id));
  }

  public void assertNotExecuted() {
    assertFalse("TransactionalAction: " + name + " was executed (but should not have been)", executedActions.contains(id));
  }
}