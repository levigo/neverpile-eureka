package com.neverpile.eureka.rest.api.document.core;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.neverpile.authorization.api.AuthorizationContext;
import com.neverpile.authorization.policy.impl.SingleValueAuthorizationContext;
import com.neverpile.eureka.api.DocumentIdGenerationStrategy;
import com.neverpile.eureka.api.index.Field;
import com.neverpile.eureka.api.index.Field.Type;
import com.neverpile.eureka.api.index.Schema;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.DocumentFacet;
import com.neverpile.eureka.rest.api.document.DocumentResource;

@Component
public class IdFacet implements DocumentFacet<String> {
  @Autowired
  private DocumentIdGenerationStrategy idGenerationStrategy;

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public String getName() {
    return "documentId";
  }

  @Override
  public JavaType getValueType(final TypeFactory f) {
    return f.constructType(String.class);
  }

  @Override
  public Set<ConstraintViolation> validateCreate(final DocumentDto requestDto) {
    String suppliedId = requestDto.getDocumentId();
    if (StringUtils.isEmpty(suppliedId)) {
      requestDto.setDocumentId(idGenerationStrategy.createDocumentId());
    } else if (!idGenerationStrategy.validateDocumentId(suppliedId)) {
      return Collections.singleton(
          new ConstraintViolation(this, "Invalid format of provided document id: " + suppliedId));
    }

    return DocumentFacet.super.validateCreate(requestDto);
  }

  @Override
  public void onRetrieve(final Document document, final DocumentDto dto) {
    dto.setDocumentId(document.getDocumentId());
    dto.add(linkTo(DocumentResource.class).slash(document.getDocumentId()).withSelfRel());
  }

  @Override
  public Schema getIndexSchema() {
    return new Field(getName(), Type.Keyword);
  }

  @Override
  public JsonNode getIndexData(final Document document) {
    return objectMapper.getNodeFactory().textNode(document.getDocumentId());
  }

  public AuthorizationContext getAuthorizationContextContribution(final Document document) {
    return new SingleValueAuthorizationContext(document.getDocumentId());
  }

  @Override
  public String toString() {
    return "DocumentFacet{" + "name=" + getName() + '}';
  }
}
