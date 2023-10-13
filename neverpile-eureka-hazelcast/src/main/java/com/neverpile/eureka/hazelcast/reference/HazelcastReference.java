package com.neverpile.eureka.hazelcast.reference;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IFunction;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.IAtomicReference;
import com.neverpile.eureka.tx.atomic.DistributedAtomicReference;

public class HazelcastReference<E extends Serializable> implements DistributedAtomicReference<E> {

  @Autowired
  private HazelcastInstance hazelcast;

  @Autowired
  private CPSubsystem cpSubsystem;

  IAtomicReference<E> ref;

  String name;

  public HazelcastReference(String name) {
    this.name = name;
  }

  @PostConstruct
  public void start(){
    this.ref = cpSubsystem.getAtomicReference(name);
  }

  @Override
  public boolean compareAndSet(E expect, E update){
    return ref.compareAndSet(expect, update);
  }
  @Override
  public E get(){
    return ref.get();
  }

  @Override
  public void set(E newValue) {
    ref.set(newValue);
  }

  @Override
  public E alterAndGet(DistributedAtomicReference.ReferenceFunction<E> fn) {
    return ref.alterAndGet((IFunction<E, E>) fn::apply);
  }
}
