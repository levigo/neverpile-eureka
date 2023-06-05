package com.neverpile.eureka.objectstore.oam;

import java.io.InputStream;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.levigo.oam.OamConnector;
import com.levigo.oam.OamObject;
import com.neverpile.eureka.api.NeverpileException;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;

@Service

public class OamObjectStoreService implements ObjectStoreService {
  private static final Logger LOGGER = LoggerFactory.getLogger(OamObjectStoreService.class);

  @Autowired
  private OamConnector oamConnector;


  private static class OamObjectStoreObject implements StoreObject {

    private OamObject oamObject;
    private ObjectName objectName;

    public OamObjectStoreObject(OamObject oamObject, ObjectName objectName) {
      this.oamObject = oamObject;
      this.objectName = objectName;
    }

    @Override
    public ObjectName getObjectName() {
      return objectName;
    }

    @Override
    public InputStream getInputStream() {
      return getOamObject().getContent();
    }

    @Override
    public String getVersion() {
      return null;
    }

    public OamObject getOamObject() {
      return oamObject;
    }
  }

  @Override
  public void put(ObjectName objectName, String version, InputStream content, long length) {
    throw new NeverpileException("Oam Objectstore supports only read only access");
  }

  @Override
  public Stream<StoreObject> list(ObjectName prefix) {
    return Collections.<StoreObject> emptyList().stream();
  }

  @Override
  public StoreObject get(ObjectName objectName) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Getting oamObject for ObjectName: " + objectName);
    }
    String[] objectIdentification = objectName.element(1).split("-");
    try {
      return new OamObjectStoreObject(oamConnector.getOamObject(objectIdentification[0], objectIdentification[1]),
          objectName);
    } catch (InputMismatchException e) {
      throw new ObjectNotFoundException(objectName, e);
    }
  }

  @Override
  public void delete(ObjectName objectName) {
    throw new NeverpileException("Oam Objectstore supports only read only access");
  }

  @Override
  public boolean checkObjectExists(ObjectName objectName) {
    try {
      get(objectName);
    } catch (ObjectNotFoundException e) {
      return false;
    }
    return true;
  }


}
