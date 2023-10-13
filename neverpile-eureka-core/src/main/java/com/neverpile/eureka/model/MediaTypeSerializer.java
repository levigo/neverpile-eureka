package com.neverpile.eureka.model;

import java.io.IOException;

import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class MediaTypeSerializer extends JsonSerializer<MediaType> {
  @Override
  public void serialize(final MediaType value, final JsonGenerator gen, final SerializerProvider serializers)
      throws IOException, JsonProcessingException {
    if(null == value)
      gen.writeNull();
    else
      gen.writeString(value.toString());
  }
}
