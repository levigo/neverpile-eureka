package com.neverpile.eureka.queue;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.neverpile.eureka.tasks.DistributedPersistentQueueType;
import com.neverpile.eureka.tasks.TaskQueue;

public abstract class AbstractProcessQueueTest {

  @DistributedPersistentQueueType("TestQueue")
  TaskQueue<EventType> processQueueCache;

  enum EventType {
    CREATE, UPDATE, DELETE
  }

  class MockListener implements TaskQueue.QueueListener<EventType> {
    private final String name;

    public MockListener() {
      this("");
    }
    
    public MockListener(final String name) {
      this.name =  testName + (name.isEmpty() ? "" : " - " + name);
    }

    @Override
    public void notifyUpdate() {
      TaskQueue.ProcessElement<EventType> job = processQueueCache.getElementToProcess();
      System.out.println("  " + name + " job: " + (job == null ? "NULL " : job.toString()));
      notificationCount.incrementAndGet();
      if (null == job) {
        return;
      }
      gotJobCount.incrementAndGet();
      processQueueCache.removeProcessedElement(job.getKey());
    }
  }

  private static AtomicInteger notificationCount;
  private static AtomicInteger gotJobCount;

  @BeforeEach
  public void prepare(TestInfo testInfo) {
    Optional<Method> testMethod = testInfo.getTestMethod();
    if (testMethod.isPresent()) {
      this.testName = testMethod.get().getName();
    }
    notificationCount = new AtomicInteger(0);
    gotJobCount = new AtomicInteger(0);
    System.out.println("Before " + testName);
  }

  @AfterEach
  public void cleanup() {
    System.out.println("After " + testName);
    TaskQueue.ProcessElement<EventType> job = processQueueCache.getElementToProcess();
    while (job != null) {
      processQueueCache.removeProcessedElement(job.getKey());
      job = processQueueCache.getElementToProcess();
    }
  }


  @DistributedPersistentQueueType("test1")
  TaskQueue<EventType> test1;

  @DistributedPersistentQueueType("test2")
  TaskQueue<EventType> test2;

  @DistributedPersistentQueueType("test3")
  TaskQueue<EventType> test3;

  @DistributedPersistentQueueType("test3")
  TaskQueue<EventType> test4;

  
  public String testName;

  @Test
  public void testThat_multipleBeansWithDistinctNamesAccessDistinctQueues() {
    Assertions.assertNotNull(test1);
    Assertions.assertNotNull(test2);
    Assertions.assertNotEquals(test1.toString(), test2.toString());
    test1.putInQueue("1", EventType.UPDATE);
    TaskQueue.ProcessElement<EventType> job = test2.getElementToProcess();
    Assertions.assertNull(job);
    job = test1.getElementToProcess();
    Assertions.assertEquals("1", job.getKey());
    test1.removeProcessedElement(job.getKey());
  }

  @Test
  public void testThat_multipleBeansWithTheSameNameAccessTheSameQueue() {
    Assertions.assertNotNull(test3);
    Assertions.assertNotNull(test4);
    Assertions.assertEquals(test3.toString(), test4.toString());
    test3.putInQueue("1", EventType.UPDATE);
    TaskQueue.ProcessElement<EventType> job = test4.getElementToProcess();
    Assertions.assertEquals("1", job.getKey());
    test4.removeProcessedElement(job.getKey());
    job = test3.getElementToProcess();
    Assertions.assertNull(job);
  }

  @Test
  public void testThat_listenersCanRegisterAndGetNotified() throws InterruptedException {

    MockListener mockListener = new MockListener("");

    processQueueCache.registerListener(mockListener);

    String id = "TESTID1";

    Assertions.assertEquals(0, notificationCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assertions.assertEquals(1, notificationCount.get());

    processQueueCache.unregisterListener(mockListener);
  }

  @Test
  public void testThat_multipleListenersGetNotified() throws InterruptedException {

    MockListener mockListener1 = new MockListener("listener 1");
    MockListener mockListener2 = new MockListener("listener 2");

    processQueueCache.registerListener(mockListener1);
    processQueueCache.registerListener(mockListener2);

    String id = "TESTID2";

    Assertions.assertEquals(0, notificationCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assertions.assertEquals(2, notificationCount.get());

    processQueueCache.unregisterListener(mockListener1);
    processQueueCache.unregisterListener(mockListener2);
  }

  @Test
  public void testThat_JobGetsExecutedOnlyOnce() throws InterruptedException {

    MockListener mockListener1 = new MockListener("listener 1");
    MockListener mockListener2 = new MockListener("listener 2");

    processQueueCache.registerListener(mockListener1);
    processQueueCache.registerListener(mockListener2);

    String id = "TESTID2";

    Assertions.assertEquals(0, gotJobCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assertions.assertEquals(1, gotJobCount.get());

    processQueueCache.unregisterListener(mockListener1);
    processQueueCache.unregisterListener(mockListener2);
  }

  @Test
  public void testThat_listenerCanReceiveMultipleEventsInSequence() throws InterruptedException {

    MockListener mockListener = new MockListener();

    processQueueCache.registerListener(mockListener);

    String id = "TESTID3";

    Assertions.assertEquals(0, notificationCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assertions.assertEquals(1, notificationCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assertions.assertEquals(2, notificationCount.get());

    processQueueCache.unregisterListener(mockListener);
  }

  @Test
  public void testThat_unregisteredListenersStopReceivingEvents() throws InterruptedException {

    MockListener mockListener = new MockListener();

    processQueueCache.registerListener(mockListener);

    String id = "TESTID4";

    Assertions.assertEquals(0, notificationCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assertions.assertEquals(1, notificationCount.get());
    processQueueCache.unregisterListener(mockListener);

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assertions.assertEquals(1, notificationCount.get());
  }
}
