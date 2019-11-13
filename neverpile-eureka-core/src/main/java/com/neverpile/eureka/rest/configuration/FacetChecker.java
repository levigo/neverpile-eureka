package com.neverpile.eureka.rest.configuration;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.neverpile.eureka.rest.api.document.DocumentFacet;

@Component
public class FacetChecker {
  @Autowired(required = false)
  private List<DocumentFacet<?>> facets;

  @PostConstruct
  public void checkFacetDupes() {
    if (null == facets)
      return;

    facets.stream().collect(Collectors.groupingBy(DocumentFacet::getName, Collectors.counting())) //
        .entrySet().stream().filter(e -> e.getValue() > 1).findAny() //
        .ifPresent(e -> {
          throw new IllegalArgumentException("There are " + e.getValue() + " facets with the name '" + e.getKey()
              + "' installed, but there must be at most one.");
        });
  }
}
