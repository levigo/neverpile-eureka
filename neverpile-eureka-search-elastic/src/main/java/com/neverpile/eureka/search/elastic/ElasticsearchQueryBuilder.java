package com.neverpile.eureka.search.elastic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.springframework.util.comparator.Comparators;

import com.neverpile.common.condition.AndCondition;
import com.neverpile.common.condition.ComparisonCondition;
import com.neverpile.common.condition.CompositeCondition;
import com.neverpile.common.condition.Condition;
import com.neverpile.common.condition.EqualsCondition;
import com.neverpile.common.condition.GreaterOrEqualToCondition;
import com.neverpile.common.condition.GreaterThanCondition;
import com.neverpile.common.condition.LessOrEqualToCondition;
import com.neverpile.common.condition.LessThanCondition;
import com.neverpile.common.condition.NotCondition;
import com.neverpile.common.condition.OrCondition;
import com.neverpile.common.condition.RangeCondition;
import com.neverpile.common.specifier.Specifier;
import com.neverpile.eureka.api.index.DocumentQuery;
import com.neverpile.eureka.api.index.Structure;

public class ElasticsearchQueryBuilder {

  public static QueryBuilder getQueryBuilderFor(final DocumentQuery searchQuery, final Structure indexSchema) {
    Condition condition = searchQuery.getConditions();

    if (condition == null) {
      return new MatchAllQueryBuilder();
    }

    return new ElasticsearchQueryBuilder(indexSchema).createGroup(condition);
  }

  private final Structure indexSchema;

  private ElasticsearchQueryBuilder(final Structure indexSchema) {
    this.indexSchema = indexSchema;
  }

  private QueryBuilder createGroup(final Condition condition) {
    if (condition instanceof CompositeCondition) {
      return createCompositeGroup((CompositeCondition<?>) condition);
    } else {
      return createFiniteGroup(condition);
    }
  }

  private QueryBuilder createCompositeGroup(final CompositeCondition<?> termQuery) {
    BoolQueryBuilder outerGroup = new BoolQueryBuilder();

    for (Condition exp : (List<Condition>) termQuery.getConditions()) {
      if (termQuery instanceof OrCondition) {
        outerGroup.should(createGroup(exp));
      } else if (termQuery instanceof AndCondition) {
        outerGroup.must(createGroup(exp));
      } else if (termQuery instanceof NotCondition) {
        outerGroup.mustNot(createGroup(exp));
      }
    }
    return outerGroup;
  }

  private static class RangeLimits {
    private String lt = null;
    private String lte = null;
    private String gt = null;
    private String gte = null;

    public String getLt() {
      return lt;
    }

    public RangeLimits setLt(final String lt) {
      this.lt = lt;
      return this;
    }

    public String getLte() {
      return lte;
    }

    public RangeLimits setLte(final String lte) {
      this.lte = lte;
      return this;
    }

    public String getGt() {
      return gt;
    }

    public RangeLimits setGt(final String gt) {
      this.gt = gt;
      return this;
    }

    public String getGte() {
      return gte;
    }

    public RangeLimits setGte(final String gte) {
      this.gte = gte;
      return this;
    }
  }

  private QueryBuilder createFiniteGroup(final Condition pred) {
    BoolQueryBuilder outerGroup = new BoolQueryBuilder();
    if (pred instanceof ComparisonCondition) {
      Map<Specifier, List<Object>> predicates = ((ComparisonCondition) pred).getPredicates();
      Map<String, RangeLimits> rangeValues = new HashMap<>();
      String key = null;
      for (Map.Entry<Specifier, List<Object>> entry : predicates.entrySet()) {
        key = entry.getKey().asString();
        List<Object> values = entry.getValue();
        values.sort(Comparators.comparable());
        if (pred instanceof LessThanCondition) {
          rangeValues.put(key, new RangeLimits().setLt(values.get(0).toString()));
        } else if (pred instanceof LessOrEqualToCondition) {
          rangeValues.put(key, new RangeLimits().setLte(values.get(0).toString()));
        } else if (pred instanceof GreaterThanCondition) {
          rangeValues.put(key, new RangeLimits().setGt(values.get(values.size() - 1).toString()));
        } else if (pred instanceof GreaterOrEqualToCondition) {
          rangeValues.put(key, new RangeLimits().setGte(values.get(values.size() - 1).toString()));
        } else if (pred instanceof EqualsCondition) {
          if (values.size() == 1 && predicates.entrySet().size() == 1) {
            // only one comparison with one possible result.
            return getTermQuery(key, values.get(0)); // No outer query needed.
          }
          for (Object value : values) {
            outerGroup.should(getTermQuery(key, value));
          }
        }
      }
      if (rangeValues.size() == 1) {
        // only one Range property.
        return getRangeQuery(key, rangeValues.get(key));
      }
      for (Map.Entry<String, RangeLimits> entry : rangeValues.entrySet()) {
        outerGroup.should(getRangeQuery(entry.getKey(), entry.getValue()));
      }

    } else if (pred instanceof RangeCondition) {
      Map<Specifier, List<Comparable<?>>> predicates = ((RangeCondition) pred).getPredicates();
      Map<String, RangeLimits> rangeValues = new HashMap<>();
      String key = null;
      for (Map.Entry<Specifier, List<Comparable<?>>> entry : predicates.entrySet()) {
        key = entry.getKey().asString();
        Comparable<?> gte = entry.getValue().get(0);
        Comparable<?> lte = entry.getValue().get(1);
        rangeValues.put(key, new RangeLimits().setLte(lte.toString()).setGte(gte.toString()));
      }
      if (rangeValues.size() == 1) {
        // only one Range property.
        return getRangeQuery(key, rangeValues.get(key));
      }
      for (Map.Entry<String, RangeLimits> entry : rangeValues.entrySet()) {
        outerGroup.should(getRangeQuery(entry.getKey(), entry.getValue()));
      }
    }
    return outerGroup;
  }

  private QueryBuilder getRangeQuery(final String field, final RangeLimits range) {
    RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(field);
    if (null != range.getLt()) {
      rangeQueryBuilder.lt(range.getLt());
    } else if (null != range.getLte()) {
      rangeQueryBuilder.lte(range.getLte());
    }

    if (null != range.getGt()) {
      rangeQueryBuilder.gt(range.getGt());
    } else if (null != range.getGte()) {
      rangeQueryBuilder.gte(range.getGte());
    }

    return rangeQueryBuilder;
  }

  private QueryBuilder getTermQuery(final String field, final Object term) {
    String suffix = "";
    if (indexSchema.isDynamicBranch(Specifier.from(field))) {
      if (null == term)
        suffix = "_null";
      else if (term instanceof Number)
        switch (term.getClass().getSimpleName()){
          case "Long":
          case "Integer":
          case "Short":
          case "Byte":
          case "BigInteger":
            suffix = "_int";
            break;
          case "Float":
          case "Double":
          case "BigDecimal":
          case "Fraction":
            suffix = "_float";
            break;
        }
      else if (term instanceof Boolean)
        suffix = "_bool";
      else
        suffix = "_text";
    }

    return new TermQueryBuilder(field + suffix, term);
  }
}
