package com.neverpile.eureka.plugin.audit.verification.merkletree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.api.ObjectStoreService;
import com.neverpile.eureka.model.ObjectName;
import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.service.AuditLogService;
import com.neverpile.eureka.plugin.audit.verification.AuditHash;
import com.neverpile.eureka.plugin.audit.verification.HashStrategyService;
import com.neverpile.eureka.tx.atomic.DistributedAtomicReference;
import com.neverpile.eureka.tx.atomic.DistributedAtomicType;

@Service
public class MerkleTreeService implements HashStrategyService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MerkleTreeService.class);

  @Autowired
  private ObjectStoreService objectStore;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AuditLogService auditLogService;

  @DistributedAtomicType("neverpile-audit-currentProof")
  DistributedAtomicReference<ProofSet> currentProof;

  private final ObjectName currentProofName = getObjectNameOf("current_proof");

  private String currentProofVersion;

  @Override
  public void addElement(AuditEvent auditEvent) {
    addElements(Collections.singletonList(auditEvent));
  }

  @Override
  public void addElements(List<AuditEvent> newLogEvents) {
    if (0 >= newLogEvents.size()) {
      return;
    }
    initCurrentProof();
    ProofSet nextProofSet = null;
    for (AuditEvent auditEvent : newLogEvents) {
      nextProofSet = currentProof.alterAndGet(input -> {
        // Add single new element.
        MerkleTree merkleTree = new MerkleTree();
        merkleTree.addLeaf(new MerkleNode(new AuditHash(auditEvent), auditEvent.getTimestamp().getTime()));
        // Get Proof for new element.
        merkleTree.buildTreeWithProof(input.getProofNodes());
        return new ProofSet(merkleTree.getProofNodes(), input.getAuditId(), auditEvent.getAuditId());
      });
      putProofNodes(nextProofSet, this.getObjectNameOf(auditEvent.getAuditId()), ObjectStoreService.NEW_VERSION);
    }
    // Update latest proof.
    if (!(null == nextProofSet) && currentProof.get().getAuditId().equals(nextProofSet.getAuditId())) {
      getProofOf(currentProofName); // to get version to update
      putProofNodes(nextProofSet, currentProofName, currentProofVersion);
    }
  }

  private void initCurrentProof() {
    // Get current proof
    ProofSet latestProof = currentProof.get();
    // If distributed atomic is not jet set.
    if (null == latestProof) {
      // Get current hash from db.
      latestProof = getProofOf(currentProofName);
      // If current hash was found in db.
      if (null != latestProof) {
        // Try to initialize atomic.
        currentProof.compareAndSet(null, latestProof);
      }
    }
  }

  @Override
  public boolean verifyHash(AuditEvent auditEvent) {
    ProofSet proof = getProofOf(getObjectNameOf(auditEvent.getAuditId()));
    ProofSet parentProof = getProofOf(getObjectNameOf(proof.getParentId()));

    // recreate proof to validate
    MerkleTree merkleTree = new MerkleTree();
    merkleTree.addLeaf(new MerkleNode(new AuditHash(auditEvent), auditEvent.getTimestamp().getTime()));
    merkleTree.buildTreeWithProof(parentProof.getProofNodes());
    List<ProofNode> checkProof = merkleTree.getProofNodes();

    if (checkProof.size() != proof.getProofNodes().size()) {
      return false;
    }
    for (int i = 0; i < checkProof.size(); i++) {
      if (!checkProof.get(i).getNodeHash().equals(proof.getProofNodes().get(i).getNodeHash())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean completeVerification() {
    MerkleTree.MerkleTreeVaildator val = new MerkleTree.MerkleTreeVaildator();
    ProofSet curProof = getProofOf(currentProofName);
    while (null != curProof.getParentId()) {
      AuditEvent curEvent = auditLogService.getEvent(curProof.getAuditId());
      ProofSet parentProof = getProofOf(getObjectNameOf(curProof.getParentId()));
      MerkleTree merkleTree = new MerkleTree();
      merkleTree.addLeaf(new MerkleNode(new AuditHash(curEvent), curEvent.getTimestamp().getTime()));
      merkleTree.buildTreeWithProof(parentProof.getProofNodes());
      if (!val.validateTreeAgainstProofNodes(merkleTree, curProof.getProofNodes())) {
        return false; // tampered audit event found.
      }
      curProof = parentProof;
    }
    return true;
  }

  private ProofSet getProofOf(ObjectName proofName) {
    ProofSet proof = null;
    ObjectStoreService.StoreObject so = objectStore.get(proofName);

    if (null != so) {
      currentProofVersion = so.getVersion();
      try {
        proof = objectMapper.readValue(so.getInputStream(),
            objectMapper.getTypeFactory().constructType(ProofSet.class));
      } catch (IOException e) {
        LOGGER.error("Failed to deserialize MerkleNode List @{} {}", proofName, e);
        throw new DocumentService.DocumentServiceException("Failed to retrieve document auditLog");
      }
    }
    return proof;
  }

  private void putProofNodes(ProofSet proof, ObjectName proofName, String version) {
    try {
      // FIXME: replace BAISO/BAOS with buffer manager for efficiency
      ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
      objectMapper.writeValue(baos, proof);

      objectStore.put(proofName, version, new ByteArrayInputStream(baos.toByteArray()), baos.size());
    } catch (ObjectStoreService.ObjectStoreException e) {
      LOGGER.error("Failed to store proof for auditLog @{} {}", proofName, e);
      throw new DocumentService.DocumentServiceException("Failed to store proof for auditLog");
    } catch (IOException e) {
      LOGGER.error("Failed to serialize ProofNode List for auditLog @{} {}", proofName, e);
      e.printStackTrace();
    }
  }

  private ObjectName getObjectNameOf(String auditId) {
    return ObjectName.of("eureka", "merkle_tree", auditId);
  }

}
