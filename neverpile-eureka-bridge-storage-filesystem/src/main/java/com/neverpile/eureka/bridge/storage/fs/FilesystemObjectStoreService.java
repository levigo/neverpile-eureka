package com.neverpile.eureka.bridge.storage.fs;

import static com.neverpile.eureka.util.ObjectNames.*;
import static java.util.stream.Collectors.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.exception.VersionMismatchException;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.tx.wal.TransactionWAL;
import com.neverpile.eureka.tx.wal.TransactionWAL.TransactionalAction;

import io.micrometer.core.annotation.Timed;

@Service
@ConditionalOnMissingBean(ObjectStoreService.class)
public class FilesystemObjectStoreService implements ObjectStoreService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemObjectStoreService.class);

  private final String newVersion = String.format("%06X", 0);

  private static final class PurgeBackup implements TransactionalAction {
    private static final long serialVersionUID = 1L;

    private final File backupFile;
    private final ObjectName objectName;

    private PurgeBackup(final File backupFile, final ObjectName objectName) {
      this.backupFile = backupFile;
      this.objectName = objectName;
    }

    @Override
    public void run() {
      try {
        readWriteLocker.writeLockObject(objectName);
        if (backupFile.exists()) {
          delete(objectName, backupFile.toPath());
        }
      } catch (IOException e) {
        throw new ObjectStoreException(objectName, "Can't delete", e);
      } finally {
        readWriteLocker.writeUnlockObject(objectName);
      }
    }

    @Override
    public String toString() {
      return "PurgeBackup [objectName=" + objectName + ", backupFile=" + backupFile + "]";
    }
  }

  private static final class RevertToBackup implements TransactionalAction {
    private static final long serialVersionUID = 1L;

    private final ObjectName objectName;
    private final File targetFile;
    private final File backupFile;

    private RevertToBackup(final ObjectName objectName, final File targetFile, final File backupFile) {
      this.objectName = objectName;
      this.targetFile = targetFile;
      this.backupFile = backupFile;
    }

    @Override
    public void run() {
      Path undoTarget = targetFile.toPath();
      Path tmp = null;
      try {
        try {
          readWriteLocker.writeLockObject(objectName);
          Files.createDirectories(undoTarget.getParent());

          tmp = undoTarget.resolveSibling(undoTarget.getFileName() + randomNameTrailer("tmp"));

          if (Files.exists(undoTarget)) {
            Files.move(undoTarget, tmp, StandardCopyOption.ATOMIC_MOVE);
          }

          Files.move(backupFile.toPath(), undoTarget, StandardCopyOption.ATOMIC_MOVE);

          if (Files.exists(tmp))
            delete(objectName, tmp);
        } catch (IOException e) {
          // try to undo revert
          if (tmp != null) {
            Files.move(tmp, undoTarget, StandardCopyOption.ATOMIC_MOVE);
          }
          throw e;
        } finally {
          readWriteLocker.writeUnlockObject(objectName);
        }
      } catch (IOException e) {
        throw new ObjectStoreException(objectName, "Can't revert to backup", e);
      }
    }

    @Override
    public String toString() {
      return "RevertToBackup [objectName=" + objectName + ", targetFile=" + targetFile + ", backupFile=" + backupFile
          + "]";
    }
  }

  private static final class UndoCreateDirectories implements TransactionalAction {
    private static final long serialVersionUID = 1L;

    private final File targetFile;

    private UndoCreateDirectories(final File targetFile) {
      this.targetFile = targetFile;
    }

    @Override
    public void run() {
      pruneEmptyDirectories(targetFile);
    }

    @Override
    public String toString() {
      return "UndoCreateDirectories [targetFile=" + targetFile + "]";
    }
  }

  private static final class UndoWriteObject implements TransactionalAction {
    private static final long serialVersionUID = 1L;

    private final ObjectName objectName;
    private final File targetFile;

    private UndoWriteObject(final ObjectName objectName, final File targetFile) {
      this.objectName = objectName;
      this.targetFile = targetFile;
    }

    @Override
    public void run() {
      try {
        readWriteLocker.writeLockObject(objectName);
        Files.deleteIfExists(targetFile.toPath());

      } catch (IOException e) {
        throw new ObjectStoreException(objectName, "Can't revert put", e);
      } finally {
        readWriteLocker.writeUnlockObject(objectName);
      }
    }

    @Override
    public String toString() {
      return "UndoWriteObject [objectName=" + objectName + ", targetFile=" + targetFile + "]";
    }
  }

  class FilesystemStoreObject implements StoreObject {
    private final ObjectName objectName;
    private final File file;
    private final String version;

    public FilesystemStoreObject(final ObjectName objectName, final File file, final String version) {
      this.objectName = objectName;
      this.file = file;
      this.version = version;
    }

    @Override
    public ObjectName getObjectName() {
      return objectName;
    }

    @Override
    public InputStream getInputStream() {
      try {
        return new LockedFileInputStream(file);
      } catch (FileNotFoundException e) {
        throw new ObjectStoreException(objectName, "Can't retrieve object stream", e);
      }
    }

    @Override
    public String getVersion() {
      return version;
    }
  }

  class LockedFileInputStream extends FilterInputStream {
    public LockedFileInputStream(final File file) throws FileNotFoundException {
      super(null);
      this.in = new FileInputStream(file);
    }

    @Override
    public void close() throws IOException {
      super.close();
    }

    @Override
    protected void finalize() throws Throwable {
      close();
      super.finalize();
    }

    @Override
    public int read() throws IOException {
      int r = super.read();
      if (r <= 0)
        close();
      return r;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      int r = super.read(b, off, len);
      if (r <= 0)
        close();
      return r;
    }
  }

  @Value("${neverpile-eureka.bridge.storage.filesystem.rootPath:./neverpile-eureka_default}")
  private String rootPath;

  private static ReadWriteLocker readWriteLocker = new SimpleReadWriteLocker();

  private static final String DELIMITER = FileSystems.getDefault().getSeparator();

  private static Path root;

  @Autowired
  private TransactionWAL wal;

  @PostConstruct
  private void init() throws IOException {
    // FIXME check for ROOT_PATH delimiter
    root = FileSystems.getDefault().getPath(rootPath).normalize();

    File rootPathAsFile = root.toFile();
    if (!rootPathAsFile.exists())
      Files.createDirectories(root);

    LOGGER.info("-----");
    LOGGER.info("Initializing neverpile eureka - Filesystem Storage Bridge ...");
    LOGGER.info("Root directory for storing objects: '{}'", root.toAbsolutePath());
    LOGGER.info("Free space left: {} GB", rootPathAsFile.getFreeSpace() / 1000000000);
    LOGGER.info("Permissions on root directory: {}{}{}", //
        rootPathAsFile.canRead() ? "Read, " : "", //
        rootPathAsFile.canWrite() ? "Write, " : "", //
        rootPathAsFile.canExecute() ? "eXecute" : "");
    LOGGER.info("-----");
  }

  @Override
  @Timed(description = "put object store element", extraTags = {
      "subsystem", "filesystem.object-store"
  }, value = "eureka.filesystem.object-store.put")
  public void put(final ObjectName objectName, String version, final InputStream content) {
    if (version.equals(NEW_VERSION)) {
      version = this.newVersion;
    }

    String currentVersion = getVersion(objectName);
    if (Long.parseLong(currentVersion, 16) != Long.parseLong(version, 16)) {
      throw new VersionMismatchException("Can't Put", version, currentVersion);
    }

    Path target = toObjectPath(objectName, version);

    File targetFile = target.toFile();

    // on rollback: remove created directory tree
    wal.appendUndoAction(new UndoCreateDirectories(targetFile));


    try {
      readWriteLocker.writeLockObject(objectName);
      // create directory hierarchy - if not existing
      Files.createDirectories(target.getParent());

      if (Files.exists(target)) {
        backupObject(objectName, target);
        delete(objectName, target);
      }

      version = String.format("%06X", Long.parseLong(version, 16) + 1);
      target = toObjectPath(objectName, version);
      targetFile = target.toFile();

      // on rollback: delete written object
      wal.appendUndoAction(new UndoWriteObject(objectName, targetFile));

      Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      LOGGER.warn("Can't put: {}", targetFile, e);
      throw new ObjectStoreException(objectName, "Can't put", e);
    } finally {
      readWriteLocker.writeUnlockObject(objectName);
    }
  }

  @Override
  public void put(final ObjectName objectName, final String version, final InputStream content, final long length) {
    put(objectName, version, content);
  }

  private void backupObject(final ObjectName objectName, final Path objectPath) {
    Path backup = objectPath.resolveSibling(objectPath.getFileName() + randomNameTrailer("backup"));

    // need to "transport" target/backup as files, since Path instances aren't serializable.
    File targetFile = objectPath.toAbsolutePath().toFile();
    File backupFile = backup.toAbsolutePath().toFile();

    // on rollback: revert to backup
    wal.appendUndoAction(new RevertToBackup(objectName, targetFile, backupFile));

    // on commit: purge backup
    wal.appendCommitAction(new PurgeBackup(backupFile, objectName));

    try {
      Files.copy(objectPath, backup);
    } catch (IOException e) {
      throw new ObjectStoreException(objectName, "Can't create backup", e);
    }
  }

  private static String randomNameTrailer(final String suffix) {
    return "." + UUID.randomUUID().toString().replaceAll("[{}]", "") + "." + suffix;
  }

  /**
   * Prune all empty directories "upwards" starting at the given target
   *
   * @param target
   */
  private static void pruneEmptyDirectories(File target) {
    while (null != target && target.isDirectory() && target.listFiles().length == 0) {
      target.delete();
      target = target.getParentFile();
    }
  }

  @Override
  @Timed(description = "list object store elements", extraTags = {
      "subsystem", "filesystem.object-store"
  }, value = "eureka.filesystem.object-store.list")
  public Stream<StoreObject> list(final ObjectName prefix) {
    try {
      // return the object of the given name...
      Path start = toPathWithRoot(prefix);
      return Files.walk(start, 1) //
          .filter(p -> !p.equals(start)) // exclude start directory
          .map(p -> p.toFile().isDirectory() ? toPrefix(p) : toStoreObject(p));
    } catch (IOException e) {
      throw new ObjectStoreException(prefix, "Can't walk file tree", e);
    }
  }

  @Override
  @Timed(description = "retrieve object store element", extraTags = {
      "subsystem", "filesystem.object-store"
  }, value = "eureka.filesystem.object-store.get")
  public StoreObject get(final ObjectName objectName) {
    final File target = new File(toObjectPath(objectName, getVersion(objectName)).toUri());
    readWriteLocker.readLockObject(objectName);
    if (!target.exists()) {
      readWriteLocker.readUnlockObject(objectName);
      return null;
    }
    String version = getVersion(objectName);
    readWriteLocker.readUnlockObject(objectName);

    // StoreObject successfully found + initialized
    return new FilesystemStoreObject(objectName, target, version);
  }

  @Override
  @Timed(description = "check object store element exists", extraTags = {
      "subsystem", "filesystem.object-store"
  }, value = "eureka.filesystem.object-store.check-exists")
  public boolean checkObjectExists(final ObjectName objectName) {
    final Path objectPath = toObjectPath(objectName, getVersion(objectName));
    readWriteLocker.readLockObject(objectName);
    if (Files.exists(objectPath)) {
      readWriteLocker.readUnlockObject(objectName);
      return true;
    }
    readWriteLocker.readUnlockObject(objectName);
    return false;
  }

  private void backupPrefix(final ObjectName objectName, final Path folderPath) throws IOException {
    Path backup = folderPath.resolveSibling(folderPath.getFileName() + randomNameTrailer("backup"));

    // need to "transport" target/backup as files, since Path instances aren't serializable.
    File targetFile = folderPath.toAbsolutePath().toFile();
    File backupFile = backup.toAbsolutePath().toFile();

    // on rollback: revert to backup
    wal.appendUndoAction(new RevertToBackup(objectName, targetFile, backupFile));

    // on commit: purge backup
    wal.appendCommitAction(new PurgeBackup(backupFile, objectName));

    copyFolder(folderPath, backup);
  }

  private static void copyFolder(final Path src, final Path dest) throws IOException {
    Files.walk(src).forEach(s -> {
      try {
        Path d = dest.resolve(src.relativize(s));
        if (Files.isDirectory(s)) {
          if (!Files.exists(d))
            Files.createDirectory(d);
          return;
        }
        Files.copy(s, d);// use flag to override existing
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  @Override
  @Timed(description = "delete object store element", extraTags = {
      "subsystem", "filesystem.object-store"
  }, value = "eureka.filesystem.object-store.delete")
  public void delete(final ObjectName objectName) {
    Path objectPath = toObjectPath(objectName, getVersion(objectName));

    if (Files.isRegularFile(objectPath)) {
      // delete targets object - just backup it
      backupObject(objectName, objectPath);
      try {
        Files.delete(objectPath);
      } catch (IOException e) {
        throw new ObjectStoreException(objectName, "Can't delete", e);
      }
    }

    Path folderPath = toPathWithRoot(objectName);
    if (Files.isDirectory(folderPath)) {
      // delete targets prefix directory
      try {
        backupPrefix(objectName, folderPath);
        delete(objectName, folderPath);
      } catch (IOException e) {
        throw new ObjectStoreException(objectName, "Can't delete", e);
      }
    }

  }

  private static void delete(final ObjectName objectName, final Path path) throws IOException {
    Files.walk(path).sorted(Comparator.reverseOrder()).forEach(p -> {
      try {
        Files.delete(p);
      } catch (IOException e) {
        throw new ObjectStoreException(objectName, "Can't delete " + p, e);
      }
    });
  }

  private StoreObject toPrefix(final Path key) {
    ObjectName objectName = toObjectName(key);

    return new StoreObject() {
      @Override
      public ObjectName getObjectName() {
        return objectName;
      }

      @Override
      public InputStream getInputStream() {
        throw new UnsupportedOperationException();
      }

      @Override
      public String getVersion() {
        return null;
      }
    };
  }

  private FilesystemStoreObject toStoreObject(final Path key) {
    ObjectName objectName = toObjectName(key);

    String version = getVersion(objectName);

    return new FilesystemStoreObject(objectName, key.toFile(), version);
  }

  private ObjectName toObjectName(final Path key) {
    String[] nameComponents = StreamSupport.stream(root.relativize(key.normalize()).spliterator(), false) //
        .map(p -> unescape(p.toString())) //
        .collect(Collectors.toList()) //
        .toArray(new String[0]);

    // strip version from last component
    if (nameComponents.length > 0) {
      Matcher m = VERSION_PATTERN.matcher(nameComponents[nameComponents.length-1]);
      if(m.matches()) {
          nameComponents[nameComponents.length-1] = m.group(1);
      }
    }
    
    return ObjectName.of(nameComponents);
  }

  private Path toObjectPath(final ObjectName objectName, final String version) {
    Path pathWithoutSuffix = toPathWithRoot(objectName);
    return pathWithoutSuffix.resolveSibling(pathWithoutSuffix.getFileName().toString() + ".$" + version);
  }

  private Path toPathWithRoot(final ObjectName objectName) {
    return Paths.get(root.toString(), toPath(objectName).toString());
  }


  private Path toPath(final ObjectName objectName) {
    return Paths.get(objectName.stream().map(s -> escape(s)).collect(joining(DELIMITER)));
  }

  private static final Pattern VERSION_PATTERN = Pattern.compile("(.*)\\.\\$(\\p{XDigit}{6})$");

  private static final String NULL_VERSION = String.format("%06X", 0);

  private String getVersion(final ObjectName objectName) {
    Path pathWithRoot = toPathWithRoot(objectName);
    Path searchFolder = pathWithRoot.getParent();
    String filenamePrefix = pathWithRoot.getFileName().toString();

    if (null == searchFolder)
      searchFolder = root;

    try {
      return Files.find(searchFolder, 1,
          (p, a) -> a.isRegularFile() && p.getFileName().toString().startsWith(filenamePrefix)) //
          .map(p -> VERSION_PATTERN.matcher(p.getFileName().toString())) //
          .filter(Matcher::matches) //
          .map(m -> m.group(2)) //
          .max(Comparator.naturalOrder()) //
          .orElse(NULL_VERSION);
    } catch (NoSuchFileException e) {
      return NULL_VERSION;
    } catch (IOException e) {
      throw new ObjectStoreException(objectName, "Can't get current version", e);
    }
  }
}