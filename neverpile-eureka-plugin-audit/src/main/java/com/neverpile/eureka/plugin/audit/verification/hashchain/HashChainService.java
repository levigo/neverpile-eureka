package com.neverpile.eureka.plugin.audit.verification.hashchain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

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

public class HashChainService implements HashStrategyService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HashChainService.class);

  @Autowired
  private ObjectStoreService objectStore;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AuditLogService auditLogService;

  @DistributedAtomicType("neverpile-audit-currentProof")
  DistributedAtomicReference<ProofChainLink> currentProof;

  private String currentHashVersion = ObjectStoreService.NEW_VERSION;
  private final ObjectName currentHashName = getObjectNameOf("current_hash");

  @Value("${neverpile-eureka.audit.verification.seed:NotSoSecretSeed}")
  private String rootNodeHashSeed = "NotSoSecretSeed";

  private ProofChainLink rootProof;

  public HashChainService() {
    this.rootProof = new ProofChainLink(new HashChainLink("root", new AuditHash(rootNodeHashSeed.getBytes())));
  }

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
    // Insert all new events as link into chain
    ProofChainLink nextProofLink = null;

    for (AuditEvent auditEvent : newLogEvents) {
      nextProofLink = currentProof.alterAndGet(input -> {
        ProofChainLink nextProof = new ProofChainLink();
        nextProof.setAuditId(auditEvent.getAuditId());
        nextProof.setParentId(input.getAuditId());
        nextProof.setLinkHash(new AuditHash(input.getLinkHash(), new AuditHash(auditEvent)));
        return nextProof;
      });
      putHashChainLink(nextProofLink, getObjectNameOf(auditEvent.getAuditId()), ObjectStoreService.NEW_VERSION);
      LOGGER.info("Proof persisted for auditId: @{}", auditEvent.getAuditId());
    }
    if (!(null == nextProofLink) && currentProof.get().getAuditId().equals(nextProofLink.getAuditId())) {
      getHashChainLink(currentHashName); // to get version to update
      putHashChainLink(nextProofLink, currentHashName, currentHashVersion);
    }
  }

  private void initCurrentProof() {
    // Get current proof
    ProofChainLink initProof = currentProof.get();
    // If distributed atomic is not jet set.
    if (null == initProof) {
      // Get current hash from db.
      initProof = getHashChainLink(currentHashName);
      // If current hash was found in db.
      if (null != initProof) {
        // Try to initialize atomic.
        currentProof.compareAndSet(null, initProof);
      } else {
        // initialize chain with new root.
        currentProof.compareAndSet(null, rootProof);
      }
    }
  }

  @Override
  public boolean verifyHash(AuditEvent auditEvent) {
    ProofChainLink link = getHashChainLink(getObjectNameOf(auditEvent.getAuditId()));
    if (null == link || null == link.getParentId()) {
      return false;
    }
    ProofChainLink proofLink = getHashChainLink(getObjectNameOf(link.getParentId()));
    return link.getLinkHash().equals(new AuditHash(proofLink.getLinkHash(), new AuditHash(auditEvent)));
  }

  @Override
  public boolean completeVerification() {
    ProofChainLink curProof = getHashChainLink(currentHashName);
    while (null != curProof.getParentId()) {
      AuditEvent curEvent = auditLogService.getEvent(curProof.getAuditId());
      ProofChainLink parentProof = getHashChainLink(getObjectNameOf(curProof.getParentId()));
      if (!curProof.getLinkHash().equals(new AuditHash(parentProof.getLinkHash(), new AuditHash(curEvent)))) {
        return false; // tampered audit event found.
      }
      curProof = parentProof;
    }
    return true;
  }

  private void putHashChainLink(ProofChainLink link, ObjectName auditHashObjectName, String version) {
    try {
      // FIXME: replace BAISO/BAOS with buffer manager for efficiency
      ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
      objectMapper.writeValue(baos, link);

      objectStore.put(auditHashObjectName, version, new ByteArrayInputStream(baos.toByteArray()), baos.size());
    } catch (ObjectStoreService.ObjectStoreException e) {
      LOGGER.error("Failed to store hash for auditLog @{}", e, auditHashObjectName);
      throw new DocumentService.DocumentServiceException("Failed to store hash for auditLog");
    } catch (IOException e) {
      LOGGER.error("Failed to serialize auditLog HashChainLink @{}", e, auditHashObjectName);
      e.printStackTrace();
    }
  }

  private ProofChainLink getHashChainLink(ObjectName auditHashObjectName) {
    ProofChainLink link = null;
    ObjectStoreService.StoreObject so = objectStore.get(auditHashObjectName);

    if (null != so) {
      currentHashVersion = so.getVersion();
      try {
        link = objectMapper.readValue(so.getInputStream(),
            objectMapper.getTypeFactory().constructType(ProofChainLink.class));
      } catch (IOException e) {
        LOGGER.error("Failed to deserialize auditLog HashChainLink @{}", e, auditHashObjectName);
        throw new DocumentService.DocumentServiceException("Failed to retrieve document auditLog");
      }
    }
    return link;
  }

  private ObjectName getObjectNameOf(String auditId) {
    return ObjectName.of("eureka", "hash_chain", auditId);
  }
}
