package com.neverpile.eureka.queue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

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
      this.name = testName.getMethodName() + (name.isEmpty() ? "" : " - " + name);
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

  @Before
  public void prepare() {
    notificationCount = new AtomicInteger(0);
    gotJobCount = new AtomicInteger(0);
    System.out.println("Before " + testName.getMethodName());
  }

  @After
  public void cleanup() {
    System.out.println("After " + testName.getMethodName());
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

  @Rule
  public TestName testName = new TestName();

  @Test
  public void testThat_multipleBeansWithDistinctNamesAccessDistinctQueues() {
    Assert.assertNotNull(test1);
    Assert.assertNotNull(test2);
    Assert.assertNotEquals(test1.toString(), test2.toString());
    test1.putInQueue("1", EventType.UPDATE);
    TaskQueue.ProcessElement<EventType> job = test2.getElementToProcess();
    Assert.assertNull(job);
    job = test1.getElementToProcess();
    Assert.assertEquals("1", job.getKey());
    test1.removeProcessedElement(job.getKey());
  }

  @Test
  public void testThat_multipleBeansWithTheSameNameAccessTheSameQueue() {
    Assert.assertNotNull(test3);
    Assert.assertNotNull(test4);
    Assert.assertEquals(test3.toString(), test4.toString());
    test3.putInQueue("1", EventType.UPDATE);
    TaskQueue.ProcessElement<EventType> job = test4.getElementToProcess();
    Assert.assertEquals("1", job.getKey());
    test4.removeProcessedElement(job.getKey());
    job = test3.getElementToProcess();
    Assert.assertNull(job);
  }

  @Test
  public void testThat_listenersCanRegisterAndGetNotified() throws InterruptedException {

    MockListener mockListener = new MockListener("");

    processQueueCache.registerListener(mockListener);

    String id = "TESTID1";

    Assert.assertEquals(0, notificationCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assert.assertEquals(1, notificationCount.get());

    processQueueCache.unregisterListener(mockListener);
  }

  @Test
  public void testThat_multipleListenersGetNotified() throws InterruptedException {

    MockListener mockListener1 = new MockListener("listener 1");
    MockListener mockListener2 = new MockListener("listener 2");

    processQueueCache.registerListener(mockListener1);
    processQueueCache.registerListener(mockListener2);

    String id = "TESTID2";

    Assert.assertEquals(0, notificationCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assert.assertEquals(2, notificationCount.get());

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

    Assert.assertEquals(0, gotJobCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assert.assertEquals(1, gotJobCount.get());

    processQueueCache.unregisterListener(mockListener1);
    processQueueCache.unregisterListener(mockListener2);
  }

  @Test
  public void testThat_listenerCanReceiveMultipleEventsInSequence() throws InterruptedException {

    MockListener mockListener = new MockListener();

    processQueueCache.registerListener(mockListener);

    String id = "TESTID3";

    Assert.assertEquals(0, notificationCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assert.assertEquals(1, notificationCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assert.assertEquals(2, notificationCount.get());

    processQueueCache.unregisterListener(mockListener);
  }

  @Test
  public void testThat_unregisteredListenersStopReceivingEvents() throws InterruptedException {

    MockListener mockListener = new MockListener();

    processQueueCache.registerListener(mockListener);

    String id = "TESTID4";

    Assert.assertEquals(0, notificationCount.get());

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assert.assertEquals(1, notificationCount.get());
    processQueueCache.unregisterListener(mockListener);

    processQueueCache.putInQueue(id, EventType.CREATE);
    Thread.sleep(500);

    Assert.assertEquals(1, notificationCount.get());
  }
}
