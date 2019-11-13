package com.neverpile.eureka.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.neverpile.eureka.model.Document;

@Component
public class EventPublisher {
  private final ApplicationEventPublisher applicationEventPublisher;

  public EventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public void publishUpdateEvent(final Document doc) {
    this.applicationEventPublisher.publishEvent(new UpdateEvent(doc));
  }

  public void publishCreateEvent(final Document doc) {
    this.applicationEventPublisher.publishEvent(new CreateEvent(doc));
  }

  public void publishDeleteEvent(final String documentId) {
    this.applicationEventPublisher.publishEvent(new DeleteEvent(documentId));
  }
}
