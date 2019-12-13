package com.neverpile.eureka.tx.atomic;

import java.io.Serializable;

public interface DistributedAtomicReference<E extends Serializable> {

    interface ReferenceFunction<E> extends Serializable{
      E apply(E input);
    }

    boolean compareAndSet(E expect, E update);

    E get();

    void set(E newValue);

    E alterAndGet(ReferenceFunction<E> fn);
}
