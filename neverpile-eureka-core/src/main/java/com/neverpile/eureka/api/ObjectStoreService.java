package com.neverpile.eureka.api;

import java.io.InputStream;
import java.util.stream.Stream;

import com.neverpile.eureka.model.ObjectName;

/**
 * The Object Store service provides generic access to a document store and therefore operations like
 * put/get/list/delete within a eureka instance. Data is stored with {@link StoreObject}s which hold an
 * {@link InputStream} with the actual data, an {@link ObjectName} as an Identifier and a version String to support
 * versioning.
 */
public interface ObjectStoreService {

  /**
   * Generic data object to store and retrieve data from the {@link ObjectStoreService}.
   * The Data contains an {@link InputStream} with the actual data, an {@link ObjectName} as an Identifier
   * and a version String to support versioning.
   */
  interface StoreObject {
    /**
     * Unique Object identifier.
     *
     * @return fully-qualified object name(= key) - example:
     * tenant/collection/document/content/object-0
     */
    ObjectName getObjectName();

    /**
     * Actual object data as a stream.
     *
     * @return initialized stream of the object's payload
     */
    InputStream getInputStream();

    /**
     * Version string to distinguish between versions of the same object
     *
     * @return Object version String.
     */
    String getVersion();
  }

  /**
   * Generic exception thrown when an error occurred while executing an operation on the object store.
   */
  public class ObjectStoreException extends NeverpileException {
    private static final long serialVersionUID = 1L;

    protected final ObjectName name;

    public ObjectStoreException(final ObjectName name, final String message, final Throwable cause) {
      super(message, cause);
      this.name = name;
    }

    public ObjectStoreException(final ObjectName name, final String message) {
      super(message);
      this.name = name;
    }

    public ObjectStoreException(final ObjectName name, final Throwable cause) {
      super(cause);
      this.name = name;
    }

    public ObjectName getName() {
      return name;
    }
  }

  /**
   * Exception thrown when trying to access an object with an {@link ObjectName} that does not exists.
   */
  public class ObjectNotFoundException extends ObjectStoreException {
    private static final long serialVersionUID = 1L;

    public ObjectNotFoundException(final ObjectName name, final Throwable cause) {
      super(name, "Object not found", cause);
    }

    public ObjectNotFoundException(final ObjectName name) {
      super(name, "Object not found");
    }
  }

  /**
   * Version String for new objects
   */
  public final static String NEW_VERSION = "";

  /**
   * Stores the stream data under the specified object name.
   *
   * @param objectName identifier (possibly with multiple name components) which will be used for
   *                   storing this object
   * @param version    Version of object to put. {@value #NEW_VERSION} for new objects
   * @param content    stream with the payload to store
   */
  default void put(final ObjectName objectName, final String version, final InputStream content) {
    put(objectName, version, content, -1L);
  }

  /**
   * Stores the stream data under the specified object name.
   *
   * @param objectName identifier (possibly with multiple name components) which will be used for
   *                   storing this object
   * @param version    Version of object to put. {@value #NEW_VERSION} for new objects
   * @param content    stream with the payload to store
   * @param length     expected estimated length of the stream
   */
  void put(ObjectName objectName, String version, InputStream content, long length);

  /**
   * Retrieve objects on a prefix-based search. Note: listings based on partial segments won't be
   * resolved, filtering will be done by fully matching name components
   *
   * @param prefix build from the name components of the provided ObjectName
   * @return stream of objects, where the specified prefix is applicable
   */
  Stream<StoreObject> list(ObjectName prefix);

  /**
   * Retrieve a single object by its fully qualified {@link ObjectName}.
   *
   * @param objectName identifier (possibly with multiple name components) which will be used for
   *                   retrieving this object
   * @return Storage Bridge-specific object containing i.a. name and stream. Return
   * <code>null</code> if the object cannot be found.
   */
  StoreObject get(ObjectName objectName);

  // FIXME: version checking?

  /**
   * Deletes an object identified by the supplied objectName without any confirmation/warning.
   *
   * @param objectName identifier (possibly with multiple name components) which will be used for
   *                   deleting this object
   */
  void delete(ObjectName objectName);


  /**
   * This method checks whether a document with the given documentID exists in the database. If this
   * is not the case, or if the method has not yet been fully implemented, it returns false by
   * default.
   *
   * @param objectName identifier (possibly with multiple name components) which will be used for
   *                   deleting this object
   * @return false if the document does not exist or the method has not yet been fully implemented
   */
  boolean checkObjectExists(ObjectName objectName);


}
