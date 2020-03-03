package com.neverpile.eureka.rest.api.document.content;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.neverpile.common.authorization.api.AuthorizationContext;
import com.neverpile.common.specifier.Specifier;
import com.neverpile.eureka.api.ContentElementService;
import com.neverpile.eureka.api.index.Array;
import com.neverpile.eureka.api.index.Field.Type;
import com.neverpile.eureka.api.index.Schema;
import com.neverpile.eureka.api.index.Structure;
import com.neverpile.eureka.model.ContentElement;
import com.neverpile.eureka.model.Document;
import com.neverpile.eureka.rest.api.document.DocumentDto;
import com.neverpile.eureka.rest.api.document.DocumentFacet;

/**
 * <h3>Authorization context</h3>
 * <p>
 * The following keys are supported:
 * </p>
 * <ul>
 * <li><code>""</code> (the empty string) -&gt; existence of this facet (always true)
 * <li><code>&lt;count&gt;</code> the number of content elements
 * <li><code>role.&lt;role&gt;</code> the number of content elements of the given role
 * <li><code>type.&lt;type or pattern&gt;</code> the number of content elements with a type
 * compatible with the given type or pattern
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "neverpile.facet.contentElement.enabled", matchIfMissing = true)
public class ContentElementFacet implements DocumentFacet<List<ContentElementDto>> {
  private final ModelMapper modelMapper = new ModelMapper();

  @Autowired
  private ContentElementService contentElementService;

  @Autowired
  private ObjectMapper objectMapper;

  @Override
  public String getName() {
    return "contentElements";
  }

  @Override
  public JavaType getValueType(final TypeFactory f) {
    return f.constructCollectionType(List.class, ContentElementDto.class);
  }

  @Override
  public void beforeUpdate(final Document currentDocument, final Document updatedDocument,
      final DocumentDto updateDto) {
    currentDocument.setContentElements( //
        updateDto.getFacetData(this) //
            .orElse(Collections.emptyList()) //
            .stream() //
            .map(dto -> modelMapper.map(dto, ContentElement.class)) //
            .collect(Collectors.toList()) //
    );
  }

  @Override
  public void onRetrieve(final Document document, final DocumentDto dto) {
    if (null != document.getContentElements() && document.getContentElements().size() != 0) {
      dto.setFacet(getName(), document.getContentElements().stream().map(ce -> {
        ContentElementDto ceDto = modelMapper.map(ce, ContentElementDto.class);
        ceDto.add(linkTo(ContentElementResource.class).slash(document.getDocumentId()) //
            .slash("content").slash(ce.getId()) //
            .withSelfRel());
        return ceDto;
      }).collect(Collectors.toList()));

      dto.add(linkTo(ContentElementResource.class).slash(document.getDocumentId()) //
          .slash("content").withRel(getName()));
    }
  }

  @Override
  public void onDelete(final Document document) {
    if (null != document.getContentElements()) {
      // propagate delete to all content elements
      for (ContentElement ce : document.getContentElements()) {
        contentElementService.deleteContentElement(document.getDocumentId(), ce.getId());
      }

      document.getContentElements().clear();
    }
  }

  public AuthorizationContext getAuthorizationContextContribution(final Document document) {
    return new AuthorizationContext() {
      @Override
      public Object resolveValue(final Specifier key) {
        if (key.empty())
          return true; // signal just the fact that the content elements facet exists

        List<ContentElement> contentElements = document.getContentElements();

        if (null == contentElements)
          return 0;

        switch (key.head()){
          case "count" :
            return contentElements.size();

          case "type" :
            if (!key.hasMore())
              return null;

            MediaType mt = MediaType.valueOf(key.suffix().head());

            return contentElements.stream().filter(e -> e.getType() != null && mt.isCompatible(e.getType())).count();

          case "role" :
            if (!key.hasMore())
              return null;

            String role = key.suffix().head();

            return contentElements.stream().filter(e -> Objects.equals(role, e.getRole())).count();
        }

        return null;
      }
    };
  }

  @Override
  public Schema getIndexSchema() {
    return new Array(new Structure() //
        .withField("id", Type.Keyword) //
        .withField("role", Type.Keyword) //
        .withField("mediaType", Type.Keyword) //
        .withField("length", Type.Integer) //
    );
  }

  @Override
  public JsonNode getIndexData(final Document document) {
    return objectMapper.createArrayNode().addAll(document.getContentElements().stream() //
        .map(ce -> objectMapper.createObjectNode() //
            .put("id", ce.getId()) //
            .put("role", ce.getRole()) //
            .put("fileName", ce.getFileName()) //
            .put("mediaType", ce.getType().getType() + "/" + ce.getType().getSubtype()) //
            .put("length", ce.getLength()) //
        ) //
        .collect(Collectors.toList()));
  }

  @Override
  public String toString() {
    return "DocumentFacet{" + "name=" + getName() + '}';
  }
}
