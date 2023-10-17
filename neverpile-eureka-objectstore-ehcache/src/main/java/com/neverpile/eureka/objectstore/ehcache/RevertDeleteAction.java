package com.neverpile.eureka.objectstore.ehcache;

import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.tx.wal.TransactionWAL;

public final class RevertDeleteAction implements TransactionWAL.TransactionalAction {

    private static final long serialVersionUID = 1L;

    private final ObjectName objectName;
    private final String fullVersion;

    public RevertDeleteAction(final ObjectName objectName,String fullVersion) {
        this.objectName = objectName;
        this.fullVersion = fullVersion;
    }

    @Override
    public void run() {
        EhcacheObjectStoreService.getInstance().revertDelete(objectName, fullVersion);
    }
}