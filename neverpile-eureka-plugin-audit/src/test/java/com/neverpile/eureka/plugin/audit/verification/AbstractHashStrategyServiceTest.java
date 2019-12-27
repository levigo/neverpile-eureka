package com.neverpile.eureka.plugin.audit.verification;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Optional;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;
import com.neverpile.eureka.tx.atomic.DistributedAtomicReference;

public abstract class AbstractHashStrategyServiceTest {

  @MockBean
  protected ObjectStoreService objectStore;

  @MockBean
  protected DistributedAtomicReference distributedAtomicReference;

  @MockBean
  protected AuditLogService auditLogService;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  private HashStrategyService hashStrategyService;

  @Test
  public void testThat_logCanBePersisted() throws IOException {
    {

      TestProofSet testProofSet = getSomeProof();
      Object proof = testProofSet.proof;

      ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
      objectMapper.writeValue(baos, proof);
      ByteArrayInputStream is = new ByteArrayInputStream(baos.toByteArray());

      ArgumentCaptor<ObjectName> currentProofObjectNameC = ArgumentCaptor.forClass(ObjectName.class);
      ArgumentCaptor<ByteArrayInputStream> newProofInputStreamC = ArgumentCaptor.forClass(ByteArrayInputStream.class);

      ObjectStoreService.StoreObject so = new ObjectStoreService.StoreObject() {
        @Override
        public ObjectName getObjectName() {
          return currentProofObjectNameC.getValue();
        }

        @Override
        public InputStream getInputStream() {
          return is;
        }

        @Override
        public String getVersion() {
          return "0";
        }
      };

      given(this.objectStore.get(currentProofObjectNameC.capture())).willReturn(so);

      doNothing().when(this.objectStore).put(any(ObjectName.class), anyString(), newProofInputStreamC.capture(),
          anyLong());

      doAnswer((Answer<Void>) invocation -> {
        DistributedAtomicReference.ReferenceFunction callback = invocation.getArgument(0);
        callback.apply(getSomeProof().proof);
        return null;
      }).when(distributedAtomicReference).alterAndGet(any(DistributedAtomicReference.ReferenceFunction.class));

      given(this.distributedAtomicReference.get()).willReturn(getSomeProof().proof);

      hashStrategyService.addElement(testProofSet.auditEvent);

      verify(this.objectStore, atLeastOnce()).put(any(ObjectName.class), anyString(), any(ByteArrayInputStream.class),
          anyLong());
    }
  }

  @Test
  public void testThat_singleEventsCanBeVerified() throws IOException {
    TestProofSet testProofSet = getSomeProof();
    Object proof1 = testProofSet.proof;
    Object proof2 = testProofSet.parentProof;

    ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
    objectMapper.writeValue(baos, proof1);
    ByteArrayInputStream is1 = new ByteArrayInputStream(baos.toByteArray());

    baos = new ByteArrayOutputStream(65535);
    objectMapper.writeValue(baos, proof2);
    ByteArrayInputStream is2 = new ByteArrayInputStream(baos.toByteArray());

    ArgumentCaptor<ObjectName> currentProofObjectNameC = ArgumentCaptor.forClass(ObjectName.class);

    ObjectStoreService.StoreObject so1 = new ObjectStoreService.StoreObject() {
      @Override
      public ObjectName getObjectName() {
        return currentProofObjectNameC.getValue();
      }

      @Override
      public InputStream getInputStream() {
        return is1;
      }

      @Override
      public String getVersion() {
        return "0";
      }
    };

    ObjectStoreService.StoreObject so2 = new ObjectStoreService.StoreObject() {
      @Override
      public ObjectName getObjectName() {
        return currentProofObjectNameC.getValue();
      }

      @Override
      public InputStream getInputStream() {
        return is2;
      }

      @Override
      public String getVersion() {
        return "0";
      }
    };

    given(this.objectStore.get(currentProofObjectNameC.capture())).willReturn(so1, so2);

    assertTrue(hashStrategyService.verifyHash(testProofSet.auditEvent));
  }

  @Test
  public void testThat_completeLogCanBeVerified() throws IOException {
    TestProofSet testProofSet = getSomeProof();
    AuditEvent event = testProofSet.auditEvent;
    Object proof1 = testProofSet.proof;
    Object proof2 = testProofSet.parentProof;

    ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
    objectMapper.writeValue(baos, proof1);
    ByteArrayInputStream is1 = new ByteArrayInputStream(baos.toByteArray());

    baos = new ByteArrayOutputStream(65535);
    objectMapper.writeValue(baos, proof2);
    ByteArrayInputStream is2 = new ByteArrayInputStream(baos.toByteArray());

    ArgumentCaptor<ObjectName> currentProofObjectNameC = ArgumentCaptor.forClass(ObjectName.class);

    ObjectStoreService.StoreObject so1 = new ObjectStoreService.StoreObject() {
      @Override
      public ObjectName getObjectName() {
        return currentProofObjectNameC.getValue();
      }

      @Override
      public InputStream getInputStream() {
        return is1;
      }

      @Override
      public String getVersion() {
        return "0";
      }
    };

    ObjectStoreService.StoreObject so2 = new ObjectStoreService.StoreObject() {
      @Override
      public ObjectName getObjectName() {
        return currentProofObjectNameC.getValue();
      }

      @Override
      public InputStream getInputStream() {
        return is2;
      }

      @Override
      public String getVersion() {
        return "0";
      }
    };

    given(this.objectStore.get(currentProofObjectNameC.capture())).willReturn(so1, so2);

    given(this.auditLogService.getEvent(anyString())).willReturn(Optional.of(event));

    assertTrue(hashStrategyService.completeVerification());
  }

  protected abstract TestProofSet getSomeProof();

  public class TestProofSet implements Serializable {
    public Serializable proof;
    public Serializable parentProof;
    public AuditEvent auditEvent;

  }
}
