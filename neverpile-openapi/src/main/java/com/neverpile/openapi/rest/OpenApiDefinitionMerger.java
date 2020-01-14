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
      try (InputStream is = fragment.getFragmentStream()) {
        JsonNode fragmentContent = yamlMapper.readTree(is);
        fragmentContent.fields().forEachRemaining(e -> mergeField(e, merged, path));
      }
    }

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
}
