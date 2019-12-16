package com.neverpile.eureka.plugin.audit.verification.impl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neverpile.eureka.plugin.audit.service.AuditEvent;
import com.neverpile.eureka.plugin.audit.verification.HashStrategyService;
import com.neverpile.eureka.plugin.audit.verification.VerificationService;
import com.neverpile.eureka.tasks.DistributedPersistentQueueType;
import com.neverpile.eureka.tasks.TaskQueue;
import com.neverpile.eureka.tx.lock.ClusterLockFactory;

@Service
public class AggregationService implements VerificationService {
  // Audit Events to be aggregated have to be at least this old.
  final long auditTimeBuffer = 60*1000; //in ms

  @DistributedPersistentQueueType("neverpile-audit-queueCache")
  TaskQueue<AuditEvent> queue;
  String distributedLockType = "neverpile-audit-aggregationLock";
  ClusterLockFactory lockFactory;
  @Autowired
  HashStrategyService auditStructure;

  private List<TaskQueue.ProcessElement<AuditEvent>> auditBlockList = new ArrayList<>();

  /**
   * Register a new audit event to process.
   * @param auditEvent new AuditEvent as value
   */
  @Override
  public void processEvent(AuditEvent auditEvent) {
    queue.putInQueue(auditEvent.getAuditId(), auditEvent);
  }

  @Override
  public boolean verifyEvent(AuditEvent auditEvent) {
    return auditStructure.verifyHash(auditEvent);
  }

  @Override
  public boolean completeVerification() {
    return auditStructure.completeVerification();
  }

  /**
   * Aggregates collectively stored {@link AuditEvent}s from distributed queue ("neverpile-audit-queueCache"), that are
   * older than {@link AggregationService#auditTimeBuffer}. Aggregated Events and the corresponding verification will
   * then be persistently sorted.
    */
  @Scheduled(cron = "0 * * * * *") // fires every minute when seconds are 0
  @Transactional
  public void aggregateEvents() {
    // Solve race condition for all eureka instances trying to aggregate the audit log.
    // (This Lock acts as a Semaphore.)
    Lock lock = lockFactory.writeLock(distributedLockType);
    if (lock.tryLock()) {
      try {
        // get all not stored AuditEvents.
        TaskQueue.ProcessElement<AuditEvent> queueElement;
        do {
          queueElement = queue.getElementToProcess();
          if (null != queueElement) {
            auditBlockList.add(queueElement);
            queue.removeProcessedElement(queueElement.getKey());
          }
        } while (null != queueElement);
        // Distinguish Events via age.
        ArrayList<AuditEvent> readyToAggregate = new ArrayList<>();
        for (TaskQueue.ProcessElement<AuditEvent> auditElement : auditBlockList) {
          if (auditElement.getValue().getTimestamp().isBefore(Instant.now().minus(auditTimeBuffer, ChronoUnit.MILLIS))) {
            // Events old enough for aggregation.
            readyToAggregate.add(auditElement.getValue());
          } else {
            // Events within time tolerance will be re added to queue.
            queue.putInQueue(auditElement.getKey(), auditElement.getValue());
          }
        }
        // Store Audits with verification.
        auditStructure.addElements(readyToAggregate);
      } finally {
        auditBlockList.clear();
        lock.unlock();
      }
    }
  }
}
