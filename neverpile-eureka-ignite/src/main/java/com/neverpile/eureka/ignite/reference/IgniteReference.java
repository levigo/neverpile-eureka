package com.neverpile.eureka.ignite.reference;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicReference;
import org.apache.ignite.IgniteLock;
import org.springframework.beans.factory.annotation.Autowired;

import com.neverpile.eureka.tx.atomic.DistributedAtomicReference;

public class IgniteReference<E extends Serializable> implements DistributedAtomicReference<E> {

  @Autowired
  private Ignite ignite;

  IgniteAtomicReference<E> ref;

  String name;

  public IgniteReference(String name) {
    this.name = name;
  }

  @PostConstruct
  public void start() {
    this.ref = ignite.atomicReference(name, null, true);
  }

  @Override
  public boolean compareAndSet(E expect, E update) {
    return ref.compareAndSet(expect, update);
  }

  @Override
  public E get() {
    return ref.get();
  }

  @Override
  public void set(E newValue) {
    ref.set(newValue);
  }

  @Override
  public E alterAndGet(ReferenceFunction<E> fn) {
    IgniteLock helperLock = ignite.reentrantLock(name + "-reference-helper-lock", true, true, true);
    try {
      helperLock.lock();
      ref.set(fn.apply(ref.get()));
      return ref.get();
    } finally {
      helperLock.unlock();
    }
  }
}
