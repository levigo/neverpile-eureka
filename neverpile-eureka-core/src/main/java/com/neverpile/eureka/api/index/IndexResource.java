package com.neverpile.eureka.api.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.DocumentFacet;

import io.micrometer.core.annotation.Timed;

@RestController
@Controller
@RequestMapping(path = "/api/v1/index", produces = {
    MediaType.APPLICATION_JSON_VALUE
})
public class IndexResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexResource.class);

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired(required = false)
  QueryService indexService;

  @Autowired(required = false)
  IndexMaintenanceService indexMaintenanceService;

  @Autowired(required = false)
  private final List<DocumentFacet<?>> facets = new ArrayList<DocumentFacet<?>>();

  @PostMapping(value = "/hard-reset")
  @Timed(description = "hard reset index", value = "eureka.index.hardReset")
  public void hardReset() {
    LOGGER.warn("Performing manual hard reset for index!");
    indexMaintenanceService.hardResetIndex();
  }

  @PostMapping(value = "/rebuild")
  @Timed(description = "rebuild index", value = "eureka.index.rebuild")
  public void rebuild() {
    LOGGER.warn("Performing manual rebuild for index!");
    indexMaintenanceService.rebuildIndex();
  }

  @GetMapping(value = "query/{query}")
  // @Timed(description = "query document", extraTags = {"operation", "retrieve", "target",
  // "document", "query"}, value="eureka.document.query")
  public List<DocumentDto> query(@PathVariable("query") final String queryJson,
      @RequestParam(name = "facets", required = false) final List<String> requestedFacets) {
    // @formatter:on
    List<DocumentDto> dtos = new ArrayList<>();
    try {
      DocumentQuery query = objectMapper.readValue(queryJson, DocumentQuery.class);
      List<Document> documents = indexService.queryDocuments(query);

      for (Document document : documents) {
        DocumentDto responseDto = new DocumentDto();
        activeFacets(requestedFacets, f -> f.onRetrieve(document, responseDto));
        dtos.add(responseDto);
      }
    } catch (IOException e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
    return dtos;
  }

  private void activeFacets(final List<String> requestedFacets, final Consumer<DocumentFacet<?>> facetConsumer) {
    (requestedFacets != null && !requestedFacets.isEmpty()
        ? facets.stream().filter(f -> requestedFacets.contains(f.getName()))
        : facets.stream().filter(DocumentFacet::includeByDefault)) //
        .forEach(facetConsumer);
  }
}
