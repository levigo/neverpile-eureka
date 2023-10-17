package com.neverpile.eureka.objectstore.ehcache;

import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.tx.wal.TransactionWAL;

public final class RevertAddAction implements TransactionWAL.TransactionalAction {

    private static final long serialVersionUID = 1L;

    private final ObjectName objectName;


    public RevertAddAction(final ObjectName objectName) {
        this.objectName = objectName;
    }

    @Override
    public void run() {
        EhcacheObjectStoreService.getInstance().revertAdd(objectName);
    }


}