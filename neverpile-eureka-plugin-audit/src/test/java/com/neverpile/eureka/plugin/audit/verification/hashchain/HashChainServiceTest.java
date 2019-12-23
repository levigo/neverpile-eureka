package com.neverpile.eureka.plugin.audit.verification.hashchain;

import java.time.Instant;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.verification.AbstractHashStrategyServiceTest;
import com.neverpile.eureka.plugin.audit.verification.AuditHash;
import com.neverpile.eureka.plugin.audit.verification.HashStrategyService;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
public class HashChainServiceTest extends AbstractHashStrategyServiceTest {
  @Override
  protected TestProofSet getSomeProof() {
    ProofChainLink proof = new ProofChainLink();
    ProofChainLink parent = (ProofChainLink) getParentOfSomeProof();

    AuditEvent ae = new AuditEvent();
    ae.setAuditId("audit1");
    ae.setTimestamp(Instant.now());

    proof.setAuditId(ae.getAuditId());
    proof.setParentId(parent.getAuditId());
    proof.setLinkHash(new AuditHash(parent.getLinkHash(), new AuditHash(ae)));

    TestProofSet testProofSet = new TestProofSet();
    testProofSet.auditEvent = ae;
    testProofSet.proof = proof;
    testProofSet.parentProof = parent;

    return testProofSet;
  }

  protected Object getParentOfSomeProof() {
    ProofChainLink proof = new ProofChainLink();
    proof.setAuditId("audit0");
    proof.setParentId(null);
    proof.setLinkHash(new AuditHash("audit0".getBytes(), "audit0".getBytes()));
    return proof;
  }
}

@Configuration
class Config {
  @Bean
  ObjectMapper getObjectMapper() {
    return new ObjectMapper();
  }

  @Bean
  HashStrategyService hashStrategyService() {
    return new HashChainService();
  }

}
