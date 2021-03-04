package com.neverpile.objectstore.oam;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.objectstore.oam.OamObjectStoreService;

public abstract class AbstractOamObjectStoreServiceTest {
  
  @Autowired
  protected ObjectStoreService objectStore;

  @Autowired
  protected TransactionTemplate transactionTemplate;

  protected ObjectName defaultName() {
    return ObjectName.of("");   
  }
  
  protected ByteArrayInputStream defaultStream() {
    return stream("");
  }

  private ByteArrayInputStream stream(final String content) {
    return new ByteArrayInputStream(("TEST CONTENT" + content).getBytes());
  }

  @Test
  void testThat_expectedServiceIsInjected() throws Exception {
    assertEquals(OamObjectStoreService.class.getName(), objectStore.getClass().getName());
  }

}
