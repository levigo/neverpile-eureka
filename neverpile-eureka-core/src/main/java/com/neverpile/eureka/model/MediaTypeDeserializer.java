package com.neverpile.eureka.model;

import java.io.IOException;

import jakarta.ws.rs.core.MediaType;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ValueDeserializer;

public class MediaTypeDeserializer extends ValueDeserializer<MediaType> {
  @Override
  public MediaType deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JacksonException {
    String mediaTypeString = p.readValueAs(String.class);
    if(null == mediaTypeString)
      return null;
    
    return MediaType.valueOf(mediaTypeString);
  }
}
