package com.neverpile.eureka.impl.tx.atomic;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import com.neverpile.eureka.tx.atomic.DistributedAtomicReference;

public class LocalAtomicReference<E extends Serializable> implements DistributedAtomicReference<E> {
  private AtomicReference<E> reference;

  String name;

  public LocalAtomicReference(String name) {
    this.name = name;
    reference = new AtomicReference<E>();
  }

  @Override
  public boolean compareAndSet(E expect, E update) {
    return reference.compareAndSet(expect, update);
  }

  @Override
  public E get() {
    return reference.get();
  }

  @Override
  public void set(E newValue) {
    reference.set(newValue);
  }

  @Override
  public E alterAndGet(ReferenceFunction<E> fn) {
    return reference.updateAndGet(fn::apply);
  }
}
