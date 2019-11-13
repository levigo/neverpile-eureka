package com.neverpile.eureka.impl.index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import com.neverpile.eureka.api.index.IndexMaintenanceService;
import com.neverpile.eureka.event.CreateEvent;
import com.neverpile.eureka.event.DeleteEvent;
import com.neverpile.eureka.event.UpdateEvent;

/**
 * A bridge that listens for document mutation events ({@link CreateEvent}, {@link UpdateEvent} and
 * {@link DeleteEvent}) and instructs an {@link IndexMaintenanceService} to perform index
 * maintenance accordingly.
 * <p>
 * Index maintenance is performed synchronously with the received events, thus, in the absence of
 * other interception, the index is updated within the initiating transaction. As a consequence, if
 * the index maintenance fails, the transaction itself will also fail.
 */
public class SynchronousIndexMaintenanceBridge {
  @Autowired
  private IndexMaintenanceService indexMaintenanceService;

  @EventListener
  public void onApplicationEvent(final CreateEvent event) {
    indexMaintenanceService.indexDocument(event.getDocument());
  }

  @EventListener
  public void onApplicationEvent(final UpdateEvent event) {
    indexMaintenanceService.updateDocument(event.getDocument());
  }

  @EventListener
  public void onApplicationEvent(final DeleteEvent event) {
    indexMaintenanceService.deleteDocument(event.getDocumentId());
  }
}
