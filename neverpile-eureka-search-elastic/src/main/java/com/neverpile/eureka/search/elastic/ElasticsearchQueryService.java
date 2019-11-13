package com.neverpile.eureka.search.elastic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.neverpile.eureka.api.DocumentService;
import com.neverpile.eureka.api.index.DocumentQuery;
import com.neverpile.eureka.api.index.QueryService;
import com.neverpile.eureka.model.Document;

@Service
public class ElasticsearchQueryService implements QueryService {
  @Autowired
  private DocumentService documentService;

  @Autowired
  private RestHighLevelClient client;

  @Autowired
  private ElasticsearchDocumentIndex elasticsearchIndex;

  @Override
  public List<Document> queryDocuments(final DocumentQuery searchQuery) {
    return documentService.getDocuments(searchDocument(searchQuery, ElasticsearchDocumentIndex.INDEX_ALIAS_READ));
  }

  @VisibleForTesting
  List<String> searchDocument(final DocumentQuery searchQuery, final String index) {
    QueryBuilder requestQueryBuilder = ElasticsearchQueryBuilder.getQueryBuilderFor(searchQuery,
        elasticsearchIndex.getIndexSchema());

    if (requestQueryBuilder == null) {
      requestQueryBuilder = new MatchAllQueryBuilder();
    }

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

    searchSourceBuilder.query(requestQueryBuilder);

    if (null != searchQuery.getSortKey()) {
      FieldSortBuilder sortBuilder = new FieldSortBuilder(searchQuery.getSortKey());
      sortBuilder.order(searchQuery.getSortOrder() == DocumentQuery.SortOrder.ASC ? SortOrder.ASC : SortOrder.DESC);

      searchSourceBuilder.sort(sortBuilder);
    }

    searchSourceBuilder.from(searchQuery.getPageNo() * searchQuery.getPageSize());
    searchSourceBuilder.size(searchQuery.getPageSize());

    SearchRequest searchRequest = new SearchRequest(index);
    searchRequest.source(searchSourceBuilder);

    return getSearchResultDocumentIds(searchRequest);
  }

  private List<String> getSearchResultDocumentIds(final SearchRequest searchRequest) {
    try {

      SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
      SearchHits hits = searchResponse.getHits();
      List<String> results = new ArrayList<>();
      for (SearchHit hit : hits) {
        results.add((String) hit.getSourceAsMap().get("documentId"));
      }
      return results;
    } catch (IOException e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }
}