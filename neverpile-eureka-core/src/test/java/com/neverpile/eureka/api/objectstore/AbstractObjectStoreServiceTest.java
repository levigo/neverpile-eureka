package com.neverpile.eureka.api.objectstore;

import static java.lang.Math.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.api.exception.VersionMismatchException;
import com.neverpile.eureka.model.ObjectName;

public abstract class AbstractObjectStoreServiceTest {

  @Autowired
  private ObjectStoreService objectStore;

  @Autowired
  TransactionTemplate transactionTemplate;

  @Rule
  public TestName name = new TestName();

  @Test
  @Transactional
  public void testThat_newElementCanBetSavedAndRetrieved() {
    objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, defaultStream());

    assertDefaultContent();
  }

  @Test
  @Transactional
  public void testThat_multipleElementsCanBetSavedAndRetrieved() {
    String name2 = "-someOtherObject";

    objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, defaultStream());
    objectStore.put(name(name2), ObjectStoreService.NEW_VERSION, stream(name2));

    assertDefaultContent();
    assertContent(name2, name2);
  }

  @Test
  @Transactional
  public void testThat_existenceOfObjectsCanBeChecked() {
    objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, defaultStream());

    assertTrue(objectStore.checkObjectExists(defaultName()));
    assertFalse(objectStore.checkObjectExists(name("Unicorn")));
  }

  @Test
  public void testThat_objectCanBeDeleted() {
    transactionTemplate.execute(status -> {
      objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, defaultStream());
      assertTrue(objectStore.checkObjectExists(defaultName()));
      objectStore.delete(defaultName());
      return null;
    });

    assertFalse(objectStore.checkObjectExists(defaultName()));
  }

  @Test
  @Transactional
  public void testThat_objectsCanBeCreatedWithSuffixes() {
    ObjectName objectName = ObjectName.of("Prefix0", "Suffix1", "Suffix2", "Suffix3", "Suffix4", "Suffix5");
    objectStore.put(objectName, ObjectStoreService.NEW_VERSION, defaultStream());
    assertTrue(objectStore.checkObjectExists(objectName));

  }

  @Test
  @Transactional
  public void testThat_objectsCanBeAddedToExistingSuffixes() {
    objectStore.put(ObjectName.of("Prefix0", "Suffix0"), ObjectStoreService.NEW_VERSION, defaultStream());
    objectStore.put(ObjectName.of("Prefix0", "Suffix1"), ObjectStoreService.NEW_VERSION, defaultStream());

    assertTrue(objectStore.checkObjectExists(ObjectName.of("Prefix0", "Suffix0")));
    assertTrue(objectStore.checkObjectExists(ObjectName.of("Prefix0", "Suffix1")));
  }

  @Test
  public void testThat_objectsCanBeDeletedViaPrefix() {
    ObjectName n1 = defaultName().append("Suffix1");
    ObjectName n2 = n1.append("Suffix2");

    transactionTemplate.execute(status -> {
      objectStore.put(n1, ObjectStoreService.NEW_VERSION, defaultStream());
      objectStore.put(n2, ObjectStoreService.NEW_VERSION, defaultStream());

      assertTrue(objectStore.checkObjectExists(n1));
      assertTrue(objectStore.checkObjectExists(n2));

      objectStore.delete(n1);
      return null;
    });

    assertFalse(objectStore.checkObjectExists(n1));
    assertFalse(objectStore.checkObjectExists(n2));
  }

  @Test
  public void testThat_objectWontGetDeletedIfSuffixGetsDeleted() {
    ObjectName p2 = defaultName().append("Prefix2");
    ObjectName s1 = p2.append("Suffix1");
    ObjectName s2 = s1.append("Suffix2");

    transactionTemplate.execute(status -> {
      objectStore.put(s1, ObjectStoreService.NEW_VERSION, defaultStream());
      objectStore.put(s2, ObjectStoreService.NEW_VERSION, defaultStream());
      objectStore.put(p2, ObjectStoreService.NEW_VERSION, defaultStream());

      assertTrue(objectStore.checkObjectExists(s1));
      assertTrue(objectStore.checkObjectExists(s2));
      assertTrue(objectStore.checkObjectExists(p2));

      objectStore.delete(s1);
      return null;
    });

    assertTrue(objectStore.checkObjectExists(p2));
    assertFalse(objectStore.checkObjectExists(s1));
    assertFalse(objectStore.checkObjectExists(s2));
  }

  @Test
  @Transactional
  public void testThat_chunkingPreservesStreamIntegrity() throws Exception {
    TestDataInputStream is = new TestDataInputStream("0123456789", 1024 * 1024 * 10); // 100MB

    MessageDigest md5 = MessageDigest.getInstance("md5");
    StreamUtils.copy(is, new DigestOutputStream(new DevNull(), md5));
    byte expected[] = md5.digest();

    is = new TestDataInputStream("0123456789", 1024 * 1024 * 10);
    objectStore.put(ObjectName.of("Test6"), ObjectStoreService.NEW_VERSION, is);

    ObjectStoreService.StoreObject so = objectStore.get(ObjectName.of("Test6"));

    md5.reset();
    StreamUtils.copy(so.getInputStream(), new DigestOutputStream(new DevNull(), md5));
    assertThat("Stream contents mismatch", expected, equalTo(md5.digest()));
  }


  @Test
  @Transactional
  public void testThat_elementCanBeOverwritten() {
    objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, defaultStream());

    objectStore.put(defaultName(), currentVersion(), stream("v2"));

    assertContent("v2");
  }


  @Test
  public void testThat_createTransactionIsSaveOnRollback() {
    transactionTemplate.execute(status -> {
      objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, stream("DO ROLLBACK"));
      status.setRollbackOnly();
      return null;
    });

    ObjectStoreService.StoreObject so = objectStore.get(defaultName());

    assertNull(so);
  }

  @Test
  public void testThat_createTransactionIsSaveOnCompletion() {
    transactionTemplate.execute(status -> {
      objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, defaultStream());
      return null;
    });

    assertDefaultContent();
  }

  @Test
  public void testThat_updateTransactionIsSaveOnRollback() {
    transactionTemplate.execute(status -> {
      objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, defaultStream());
      return null;
    });

    transactionTemplate.execute(status -> {
      objectStore.put(defaultName(), currentVersion(), stream("DO ROLLBACK"));
      status.setRollbackOnly();
      return null;
    });

    assertDefaultContent();
  }

  @Test
  public void testThat_updateTransactionIsSaveOnCompletion() {
    transactionTemplate.execute(status -> {
      objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, defaultStream());
      return null;
    });

    String testContent2 = "DO NOT ROLLBACK";
    transactionTemplate.execute(status -> {
      objectStore.put(defaultName(), currentVersion(), stream(testContent2));
      return null;
    });

    assertContent(testContent2);
  }

  @Test(expected = VersionMismatchException.class)
  @Transactional
  public void testTact_VersionControlWorks() {
    objectStore.put(defaultName(), ObjectStoreService.NEW_VERSION, defaultStream());
    String initialVersion = currentVersion();
    objectStore.put(defaultName(), initialVersion, stream("v2"));
    objectStore.put(defaultName(), initialVersion, stream("v3"));
  }

  @Test
  @Transactional
  public void testThat_ObjectNamesCanBeListedViaPrefix() {
    // unrelated tree as a sibling of the list root
    ObjectName sibling = defaultName().append("sibling");

    // list root
    ObjectName r = defaultName().append("root");

    // single child object
    ObjectName r_o1 = r.append("o1");

    // child object which is also a prefix two others
    ObjectName r_p1 = r.append("p1");
    ObjectName r_p1_o2 = r_p1.append("o2");
    ObjectName r_p1_o3 = r_p1.append("o3");

    // child objects with a prefix that isn't an object itself
    ObjectName r_p2 = r.append("p2");
    ObjectName r_p2_o4 = r_p2.append("o4");
    ObjectName r_p2_o5 = r_p2.append("o5");

    // store all
    Stream.of(sibling, r_o1, r_p1, r_p1_o2, r_p1_o3, r_p2_o4, r_p2_o5).forEach(
        n -> objectStore.put(n, ObjectStoreService.NEW_VERSION, defaultStream()));

    // r_p1 appears twice: once for the object, once for the prefix!
    assertThat(objectStore.list(r) //
        .map(o -> o.getObjectName()) //
        .collect(Collectors.toList()), //
        containsInAnyOrder(r_o1, r_p1, r_p1, r_p2));

    // there's no common prefix, just two matches
    assertThat(objectStore.list(r_p1) //
        .map(o -> o.getObjectName()) //
        .collect(Collectors.toList()), //
        containsInAnyOrder(r_p1_o2, r_p1_o3));
  }

  @Test
  @Transactional
  public void testThat_ObjectNamesSupportDangerousCharacters() {
    ObjectName r = defaultName();

    String weirdStuff = "/\\$%&_+.,~|\"':^😀👍🏻\u0000\u0001";

    // weird characters in direct object
    ObjectName weird = r.append("o1" + weirdStuff);

    // weird characters in sub-object
    ObjectName p1 = r.append("p1");
    ObjectName p1_weird = p1.append("o2" + weirdStuff);

    // "dangerous" path characters
    ObjectName dot = r.append(".");
    ObjectName dotdot = r.append("..");
    ObjectName dotslash = r.append("./");
    ObjectName dotdotslash = r.append("../");

    // store all
    Stream.of(weird, p1, p1_weird, dot, dotdot, dotslash, dotdotslash) //
        .forEach(n -> objectStore.put(n, ObjectStoreService.NEW_VERSION, stream(name.toString())));

    // verify contents
    Stream.of(weird, p1, p1_weird, dot, dotdot, dotslash, dotdotslash) //
        .forEach(n -> assertContent(n, name.toString()));

    assertThat(objectStore.list(r) //
        .map(o -> o.getObjectName()) //
        .collect(Collectors.toList()), //
        containsInAnyOrder(weird, p1, p1, dot, dotdot, dotslash, dotdotslash));

    assertThat(objectStore.list(p1) //
        .map(o -> o.getObjectName()) //
        .collect(Collectors.toList()), //
        containsInAnyOrder(p1_weird));
  }

  @Test
  @Transactional
  public void testThat_AllObjectsCanBeListed() {
    objectStore.put(defaultName().append("a"), ObjectStoreService.NEW_VERSION, defaultStream());
    objectStore.put(defaultName().append("b"), ObjectStoreService.NEW_VERSION, defaultStream());
    objectStore.put(defaultName().append("b").append("1"), ObjectStoreService.NEW_VERSION, defaultStream());
    objectStore.put(defaultName().append("b").append("2"), ObjectStoreService.NEW_VERSION, defaultStream());
    objectStore.put(defaultName().append("c").append("1").append("1"), ObjectStoreService.NEW_VERSION, defaultStream());

    // We expect: a, b (the object), b (the prefix) and c
    assertEquals(4, objectStore.list(defaultName()).count());
  }


  class TestDataInputStream extends InputStream {
    private final byte[] sample;
    private long pos = 0;
    private final long total;
    private final Stack<Long> marks = new Stack<>();

    TestDataInputStream(final String content, final int times) {
      super();
      this.sample = content.getBytes();
      this.total = (long) sample.length * times;
    }

    @Override
    public int read() {
      return pos < total ? sample[(int) (pos++ % sample.length)] : -1;
    }

    @Override
    public int read(@NotNull final byte[] b, final int off, final int len) {
      if (b == null) {
        throw new NullPointerException();
      } else if (off < 0 || len < 0 || len > b.length - off) {
        throw new IndexOutOfBoundsException();
      } else if (len == 0) {
        return 0;
      }

      // Modify len to satisfy only a random length between 1 and len
      int toRead = (int) ((Math.random() * (len - 1)) + 1);
      toRead = max(toRead, min(toRead, 1));

      int c = read();
      if (c == -1) {
        return -1;
      }
      b[off] = (byte) c;

      int i = 1;
      for (; i < toRead; i++) {
        c = read();
        if (c == -1) {
          break;
        }
        b[off + i] = (byte) c;
      }
      return i;
    }

    @Override
    public int available() {
      return (total - pos > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) (total - pos));
    }

    @Override
    public boolean markSupported() {
      return true;
    }

    @Override
    public synchronized void mark(final int readlimit) {
      marks.push(pos);
    }

    @Override
    public synchronized void reset() throws IOException {
      if (marks.isEmpty())
        throw new IllegalStateException();

      pos = marks.pop();
    }
  }

  static class DevNull extends OutputStream {
    @Override
    public void write(final int b) {
      // nothing to do
    }
  }

  private String stringInputStreamToString(final InputStream is) {
    StringBuilder textBuilder = new StringBuilder();
    try (
        Reader reader = new BufferedReader(new InputStreamReader(is, Charset.forName(StandardCharsets.UTF_8.name())))) {
      int c;
      while ((c = reader.read()) != -1) {
        textBuilder.append((char) c);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return textBuilder.toString();
  }

  private String currentVersion() {
    return objectStore.get(defaultName()).getVersion();
  }

  private ObjectName defaultName() {
    return name("");
  }

  private ObjectName name(final String suffix) {
    return ObjectName.of(name.getMethodName() + suffix);
  }

  private ByteArrayInputStream stream(final String content) {
    return new ByteArrayInputStream(("TEST CONTENT" + content).getBytes());
  }

  private ByteArrayInputStream defaultStream() {
    return stream("");
  }

  private void assertDefaultContent() {
    assertContent("");
  }

  private void assertContent(final String stuff) {
    assertContent("", stuff);
  }

  private void assertContent(final String name, final String content) {
    assertContent(name(name), content);
  }

  private void assertContent(final ObjectName objectName, final String content) {
    assertEquals("TEST CONTENT" + content, stringInputStreamToString(objectStore.get(objectName).getInputStream()));
  }
}
