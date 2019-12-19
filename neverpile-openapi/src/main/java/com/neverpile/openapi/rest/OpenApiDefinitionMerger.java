package com.neverpile.openapi.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.neverpile.common.openapi.OpenApiFragment;
import com.neverpile.common.specifier.Specifier;

public class OpenApiDefinitionMerger {
  private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiDefinitionMerger.class);

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  public ObjectNode mergeFragments(final Collection<OpenApiFragment> fragments) throws IOException {
    ObjectNode merged = yamlMapper.createObjectNode();
    Specifier path = Specifier.from("");

    for (OpenApiFragment fragment : fragments) {
      try (InputStream is = fragment.getResource().getInputStream()) {
        JsonNode fragmentContent = yamlMapper.readTree(is);
        fragmentContent.fields().forEachRemaining(e -> mergeField(e, merged, path));
      }
    }

    // resolveRefs(merged, merged, path);

    return merged;
  }

  private void mergeField(final Map.Entry<String, JsonNode> entry, final ObjectNode merged, final Specifier path) {
    String name = entry.getKey();
    JsonNode value = entry.getValue();
    JsonNode existing = merged.path(name);
    Specifier subPath = path.append(name);

    LOGGER.debug("Merging at {}:\n    {}\n  into \n    {} ", subPath, value, existing);

    switch (value.getNodeType()){
      case STRING :
      case BOOLEAN :
      case NULL :
      case NUMBER :
      case BINARY :
        if (!existing.isMissingNode() && !existing.equals(value)) {
          LOGGER.warn("Value already extsis at {}: {}. Ignoring conflicting entry {}", subPath, existing, value);
        } else {
          merged.set(name, value);
        }
        break;

      case ARRAY :
        if (existing.isMissingNode()) {
          merged.set(name, value);
        } else if (existing.isArray()) {
          // arrays merge naturally
          ((ArrayNode) existing).addAll((ArrayNode) value);
        } else {
          LOGGER.warn("Value already extsis at {} and is not an array: {}. Ignoring conflicting entry {}", subPath,
              existing, value);
        }
        break;

      case OBJECT :
        if (!existing.isMissingNode()) {
          // objects are merged recursively
          value.fields().forEachRemaining(e -> mergeField(e, (ObjectNode) existing, subPath));
        } else {
          merged.set(name, value);
        }
        break;

      case MISSING :
      case POJO :
        LOGGER.warn("Unexpected node at {}: {} - ignoring it", subPath, value);
        break;
    }
  }
  //
  // private void resolveRefs(final JsonNode node, final ObjectNode root, final Specifier path) {
  // switch (node.getNodeType()){
  // case ARRAY :
  // ArrayNode a = (ArrayNode) node;
  // for (int i = 0; i < a.size(); i++) {
  // JsonNode element = a.path(i);
  // if (isRef(element))
  // a.set(i, resolveRef(element, root, path.append("[" + i + "]")));
  // }
  // break;
  //
  // case OBJECT :
  // ((ObjectNode) node).fields().forEachRemaining(e -> {
  // String name = e.getKey();
  // JsonNode value = e.getValue();
  // if (isRef(value))
  // e.setValue(resolveRef(value, root, path.append(name)));
  // });
  // break;
  //
  // default :
  // // nothing to do
  // }
  // }
  //
  // private JsonNode resolveRef(final JsonNode refNode, final ObjectNode root, final Specifier
  // path) {
  // // TODO Auto-generated method stub
  // return refNode;
  // }
  //
  // private boolean isRef(final JsonNode child) {
  // // TODO Auto-generated method stub
  // return false;
  // }
}
