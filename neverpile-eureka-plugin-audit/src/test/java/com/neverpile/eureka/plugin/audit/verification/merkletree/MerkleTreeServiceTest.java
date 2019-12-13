package com.neverpile.eureka.plugin.audit.verification.merkletree;

import java.util.Date;

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
public class MerkleTreeServiceTest extends AbstractHashStrategyServiceTest {
  @Override
  protected TestProofSet getSomeProof() {
    ProofSet parent = (ProofSet) getParentOfSomeProof();
    MerkleTree mtProof = new MerkleTree();

    AuditEvent ae = new AuditEvent();
    ae.setAuditId("audit1");
    ae.setTimestamp(new Date());

    mtProof.addLeaf(new MerkleNode(new AuditHash(ae), 7));
    mtProof.buildTreeWithProof(parent.getProofNodes());

    ProofSet proof = new ProofSet(mtProof.getProofNodes(), "foo3", "audit1");

    TestProofSet testProofSet = new TestProofSet();
    testProofSet.auditEvent = ae;
    testProofSet.proof = proof;
    testProofSet.parentProof = parent;
    return testProofSet;
  }

  protected Object getParentOfSomeProof() {
    MerkleTree mtProof = TreeTest.getMerkleTreeWith7Nodes();
    mtProof.buildTreeFromLeaves();
    ProofSet proof = new ProofSet(mtProof.getProofNodes(), null, "foo3");
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
    return new MerkleTreeService();
  }

}
