package com.neverpile.eureka.ignite;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.apache.ignite.IgniteSpringBean;

public class IgniteInstanceConfiguration extends IgniteSpringBean {
  static {
    System.setProperty("IGNITE_QUIET", "false");
    System.setProperty("IGNITE_PERFORMANCE_SUGGESTIONS_DISABLED", "true");
  }
  
  @Override
  public void afterSingletonsInstantiated() {
    // not here
  }

  @PostConstruct
  public void init() {
    super.afterSingletonsInstantiated();

    active(true);
  }
  
  @PreDestroy
  public void destroy() throws Exception {
    super.destroy();
  }
}
