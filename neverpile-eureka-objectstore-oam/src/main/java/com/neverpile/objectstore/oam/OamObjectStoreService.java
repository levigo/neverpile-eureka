package com.neverpile.objectstore.oam;

import java.io.InputStream;
import java.util.stream.Stream;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OamObjectStoreService implements ObjectStoreService {

  @Override
  public boolean checkObjectExists(ObjectName objectName) {
    return true;
  }

  @Override
  public void delete(ObjectName objectName) {
    logger.debug("ObjectName: {}", objectName);
    return;
  }

  @Override
  public StoreObject get(ObjectName objectName) {
    logger.debug("ObjectName: {}", objectName);
    return new StoreObject() {
      
      @Override
      public String getVersion() {
        return null;
      }
      
      @Override
      public ObjectName getObjectName() {
        return null;
      }
      
      @Override
      public InputStream getInputStream() {
        return null;
      }
      
    };
  }

  @Override
  public Stream<StoreObject> list(ObjectName prefix) {
    logger.debug("ObjectName: {}", prefix);
    return null;
  }

  @Override
  public void put(ObjectName objectName, String version, InputStream content, long length) {
    logger.debug("ObjectName: {}", objectName);
    return;
  }

}
