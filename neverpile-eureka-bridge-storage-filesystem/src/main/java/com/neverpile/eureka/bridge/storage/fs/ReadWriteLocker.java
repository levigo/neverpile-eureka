package com.neverpile.eureka.bridge.storage.fs;

import com.neverpile.eureka.model.ObjectName;

public interface ReadWriteLocker {

  public void writeLockObject(ObjectName targetFile);

  public void readLockObject(ObjectName targetFile);

  public void writeUnlockObject(ObjectName targetFile);

  public void readUnlockObject(ObjectName targetFile);
}
