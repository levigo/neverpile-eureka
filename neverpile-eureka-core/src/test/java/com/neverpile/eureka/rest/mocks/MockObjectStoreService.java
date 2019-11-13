package com.neverpile.eureka.rest.mocks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.util.StreamUtils;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;

public class MockObjectStoreService implements ObjectStoreService {
  public final Map<ObjectName, byte[]> streams = new HashMap<>();

  public MockObjectStoreService reset() {
    streams.clear();
    return this;
  }

  @Override
  public void put(final ObjectName objectName, final String version, final InputStream content) {
    try {
      streams.put(objectName, StreamUtils.copyToByteArray(content));
    } catch (IOException e) {
      throw new ObjectStoreException(objectName, "Can't put", e);
    }
  }

  @Override
  public void put(final ObjectName objectName, final String version, final InputStream content, final long length) {
    put(objectName, version, content);
  }

  @Override
  public Stream<StoreObject> list(final ObjectName prefix) {
    return streams.entrySet().stream().filter(e -> prefix.isPrefixOf(e.getKey())).map(e -> new StoreObject() {
      @Override
      public ObjectName getObjectName() {
        return e.getKey();
      }

      @Override
      public InputStream getInputStream() {
        return new ByteArrayInputStream(e.getValue());
      }

      @Override
      public String getVersion() {
        return "0";
      }
    });
  }

  @Override
  public StoreObject get(final ObjectName objectName) {
    byte[] data = streams.get(objectName);

    if (null == data)
      return null;

    return new StoreObject() {
      @Override
      public ObjectName getObjectName() {
        return objectName;
      }

      @Override
      public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
      }

      @Override
      public String getVersion() {
        return "0";
      }
    };
  }

  @Override
  public void delete(final ObjectName objectName) {
    streams.remove(objectName);
  }

  @Override
  public boolean checkObjectExists(final ObjectName objectName) {
    return streams.containsKey(objectName);
  }
}
