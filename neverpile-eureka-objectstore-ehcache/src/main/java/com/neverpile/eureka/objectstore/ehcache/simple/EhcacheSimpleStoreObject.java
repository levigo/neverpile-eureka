package com.neverpile.eureka.objectstore.ehcache.simple;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.ehcache.Cache;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.objectstore.ehcache.EhcacheHelper;

public class EhcacheSimpleStoreObject implements ObjectStoreService.StoreObject {

    public static final String SIMPLE_VERSION = String.format("%06X", 1);

    private final ObjectName objectName;
    private final Cache<String, byte[]> cache;

    public EhcacheSimpleStoreObject(final ObjectName objectName, Cache<String, byte[]> cache) {
        this.objectName = objectName;
        this.cache = cache;
    }

    @Override
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(cache.get(EhcacheHelper.getReadableObjectName(objectName)));
    }

    @Override
    public String getVersion() {
        return SIMPLE_VERSION;
    }

    @Override
    public String toString() {
        return "EhcacheStoreObject{" +
                "objectName=" + EhcacheHelper.getReadableObjectName(objectName) +
                '}';
    }
}