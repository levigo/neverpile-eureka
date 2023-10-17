package com.neverpile.eureka.objectstore.ehcache;

import com.neverpile.eureka.model.ObjectName;

public class EhcacheHelper {

    public static final String separator = "#";

    public static String getReadableObjectName(ObjectName objectName) {
        StringBuilder readableName = new StringBuilder();
        for (int i = 0; i < objectName.to().length; i++) {
            String part = objectName.to()[i];
            if (i > 0) {
                readableName.append(separator);
            }
            readableName.append(part);
        }
        return readableName.toString();
    }


}
