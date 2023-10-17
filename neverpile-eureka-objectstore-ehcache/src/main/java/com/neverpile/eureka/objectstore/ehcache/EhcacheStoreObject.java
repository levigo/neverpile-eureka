package com.neverpile.eureka.objectstore.ehcache;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.ehcache.Cache;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;

public class EhcacheStoreObject  implements ObjectStoreService.StoreObject {

    private final ObjectName objectName;
    private final String version;
    private final Cache<String, byte[]> cache;

    public EhcacheStoreObject(final ObjectName objectName, final String version, Cache<String, byte[]> cache) {
        this.objectName = objectName;
        this.version = version;
        this.cache = cache;
    }

    @Override
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(cache.get(EhcacheHelper.getReadableObjectName(objectName) + version));
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "EhcacheStoreObject{" +
                "objectName=" + EhcacheHelper.getReadableObjectName(objectName) +
                ", version='" + version + '\'' +
                '}';
    }
}