package com.neverpile.eureka.api.index;

import com.neverpile.common.condition.AndCondition;
import com.neverpile.common.condition.Condition;

/**
 * Query Object to find Documents based on certain, optionally nested, conditions and the returned
 * format e.g. paging and sort order.
 */
public class DocumentQuery {
  public static class DocumentQueryBuilder {
    private final DocumentQuery query = new DocumentQuery(new AndCondition(), null, SortOrder.ASC, 0, 1000);

    public static class SortOrderBuilder {
      private final DocumentQueryBuilder dqb;

      SortOrderBuilder(final DocumentQueryBuilder dqb) {
        this.dqb = dqb;
      }

      public DocumentQueryBuilder asc() {
        dqb.query.setSortOrder(SortOrder.ASC);
        return dqb;
      }

      public DocumentQueryBuilder desc() {
        dqb.query.setSortOrder(SortOrder.DESC);
        return dqb;
      }
    }
    
    public DocumentQueryBuilder and(final Condition condition) {
      query.getConditions().addCondition(condition);
      return this;
    }

    public SortOrderBuilder orderBy(final String sortkey) {
      query.setSortKey(sortkey);
      return new SortOrderBuilder(this);
    }
    
    public DocumentQueryBuilder pageNo(final int pageNo) {
      query.setPageNo(pageNo);
      return this;
    }

    public DocumentQueryBuilder pageSize(final int pageSize) {
      query.setPageSize(pageSize);
      return this;
    }

    public DocumentQuery build() {
      return query;
    }
  }

  public static DocumentQueryBuilder query(final Condition condition) {
    return new DocumentQueryBuilder().and(condition);
  }

  private AndCondition conditions;

  private String sortKey;

  private SortOrder sortOrder;

  private int pageNo;

  private int pageSize;

  public DocumentQuery() {
  }

  public DocumentQuery(final AndCondition conditions, final String sortKey, final SortOrder sortOrder, final int pageNo, final int pageSize) {
    this.conditions = conditions;
    this.sortKey = sortKey;
    this.sortOrder = sortOrder;
    this.pageNo = pageNo;
    this.pageSize = pageSize;
  }

  public enum SortOrder {
    ASC, DESC
  }

  public AndCondition getConditions() {
    return conditions;
  }

  public void setConditions(final AndCondition conditions) {
    this.conditions = conditions;
  }

  public String getSortKey() {
    return sortKey;
  }

  public void setSortKey(final String sortKey) {
    this.sortKey = sortKey;
  }

  public SortOrder getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(final SortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  public int getPageNo() {
    return pageNo;
  }

  public void setPageNo(final int pageNo) {
    this.pageNo = pageNo;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(final int pageSize) {
    this.pageSize = pageSize;
  }

}
