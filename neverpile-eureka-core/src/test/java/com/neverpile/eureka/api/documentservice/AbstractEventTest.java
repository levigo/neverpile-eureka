package com.neverpile.eureka.api.documentservice;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.event.AggregatedUpdateEvent;
import com.neverpile.eureka.event.CreateEvent;
import com.neverpile.eureka.event.DeleteEvent;
import com.neverpile.eureka.event.UpdateEvent;
import com.neverpile.eureka.model.Document;


public abstract class AbstractEventTest {

  protected static final String D = "aTestDocument";
  
  protected static class EventCounter {
      public EventCounter() {
      }
  
      private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
  
      public Map<String, AtomicInteger> getMap() {
        return counters;
      }
  
      public void clear() {
        this.counters.clear();
      }
  
      public void increment(final Object event) {
        String eventName = event.getClass().getSimpleName().replaceAll("Event$", "");
        counters.computeIfAbsent(eventName, k -> new AtomicInteger()).incrementAndGet();
  
        synchronized (this) {
          notifyAll();
        }
      }
  
      @EventListener
      public void onApplicationEvent(final CreateEvent event) {
        increment(event);
      }
  
      @EventListener
      public void onApplicationEvent(final UpdateEvent event) {
        increment(event);
      }
  
      @EventListener
      public void onApplicationEvent(final DeleteEvent event) {
        increment(event);
      }
  
      @EventListener
      public void onApplicationEvent(final AggregatedUpdateEvent event) {
        increment(event);
      }
  
      public int count(final String name) {
        return counters.getOrDefault(name, new AtomicInteger()).get();
      }
  
      public void awaitMatch(final String name, final Matcher<Integer> m, final long timeout)
          throws TimeoutException, InterruptedException {
        long start = System.currentTimeMillis();
  
        while (!m.matches(count(name)) && System.currentTimeMillis() < start + timeout) {
          synchronized (this) {
            wait(timeout - (System.currentTimeMillis() - start));
          }
        }
  
        if (!m.matches(count(name))) {
          throw new TimeoutException("Timed out waiting for " + name + " to match " + m);
        }
      }
    }

  @MockBean
  protected ObjectStoreService objectStoreService;
  @Autowired
  DocumentService documentService;
  @Autowired
  protected EventCounter eventCounter;
  @Autowired
  TransactionTemplate transactionTemplate;
  @Autowired
  ObjectMapper mapper;

  public AbstractEventTest() {
    super();
  }

  @Test
  public void test_CreateEvent() {
    Document doc = new Document();
    transactionTemplate.execute(s -> documentService.createDocument(doc));
    assertThat(eventCounter.count("Create"), equalTo(1));
    assertThat(eventCounter.count("Update"), equalTo(0));
    assertThat(eventCounter.count("Delete"), equalTo(0));
  }

  @Test
  public void test_UpdateEvent() {
    Document doc = new Document();
    doc.setDocumentId(D);
  
    mockExistingDocument();
  
    transactionTemplate.execute(s -> documentService.update(doc));
    assertThat(eventCounter.count("Create"), equalTo(0));
    assertThat(eventCounter.count("Update"), equalTo(1));
    assertThat(eventCounter.count("Delete"), equalTo(0));
  }

  @Test
  public void test_AggregateUpdateEvent() throws Exception {
    Document doc = new Document();
    doc.setDocumentId(D);
    
    mockExistingDocument();
    
    transactionTemplate.execute(s -> documentService.update(doc));
    doc.setVersionTimestamp(null); // disable version checking for next update 
    
    transactionTemplate.execute(s -> documentService.update(doc));
    
    assertThat(eventCounter.count("Create"), equalTo(0));
    assertThat(eventCounter.count("Update"), equalTo(2));
    assertThat(eventCounter.count("Delete"), equalTo(0));
    assertThat(eventCounter.count("AggregatedUpdate"), equalTo(0));
  
    eventCounter.awaitMatch("AggregatedUpdate", equalTo(1), 10000);
  
    assertThat(eventCounter.count("Create"), equalTo(0));
    assertThat(eventCounter.count("Update"), equalTo(2));
    assertThat(eventCounter.count("Delete"), equalTo(0));
    assertThat(eventCounter.count("AggregatedUpdate"), equalTo(1));
  }

  protected abstract void mockExistingDocument();

  @Test
  public void test_DeleteEvent() {
    mockExistingDocument();
    transactionTemplate.execute(s -> documentService.deleteDocument(D));
    assertThat(eventCounter.count("Create"), equalTo(0));
    assertThat(eventCounter.count("Update"), equalTo(0));
    assertThat(eventCounter.count("Delete"), equalTo(1));
  }

  @Before
  public void resetDocumentIdGenerationStrategy() {
    eventCounter.clear();
  }

}