package com.neverpile.eureka.tx.wal.util;

import com.neverpile.eureka.tx.wal.WriteAheadLog.EventType;

public class EventEntry extends Entry {
  private static final long serialVersionUID = 1L;

  public EventType type;

  public EventEntry(final String txId, final EventType type) {
    super(txId);
    this.type = type;
  }

  @Override
  public String toString() {
    return type.name();
  }
}