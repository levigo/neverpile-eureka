package com.neverpile.eureka.event;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UpdateEventAggregator {

  private final ApplicationEventPublisher applicationEventPublisher;

  private final ConcurrentHashMap<String, Timer> aggregateTimers = new ConcurrentHashMap<>();

  public UpdateEventAggregator(final ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @EventListener
  public void onApplicationEvent(final UpdateEvent event) {
    String docId = event.getDocumentId();

    if (docId == null) {
      return;
    }

    aggregateTimers.compute(docId, (i, t) -> {
      if (null == t) {
        t = new Timer();
      } else {
        t.cancel();
        t = new Timer();
      }

      t.schedule(new TimerTask() {
        @Override
        public void run() {
          aggregateTimers.remove(docId);
          applicationEventPublisher.publishEvent(new AggregatedUpdateEvent(event.getDocument()));
        }
      }, TimeUnit.SECONDS.toMillis(1));
      return t;
    });
  }
}
