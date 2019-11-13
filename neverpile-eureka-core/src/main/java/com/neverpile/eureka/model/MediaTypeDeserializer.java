package com.neverpile.eureka.model;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class MediaTypeDeserializer extends JsonDeserializer<MediaType> {
  @Override
  public MediaType deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
    String mediaTypeString = p.readValueAs(String.class);
    if(null == mediaTypeString)
      return null;
    
    return MediaType.valueOf(mediaTypeString);
  }
}
