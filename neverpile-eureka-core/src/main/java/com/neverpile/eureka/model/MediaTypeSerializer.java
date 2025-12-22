package com.neverpile.eureka.model;

import java.io.IOException;

import jakarta.ws.rs.core.MediaType;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class MediaTypeSerializer extends ValueSerializer<MediaType> {
  @Override
  public void serialize(final MediaType value, final JsonGenerator gen, final SerializationContext serializers)
      throws IOException, JacksonException {
    if(null == value)
      gen.writeNull();
    else
      gen.writeString(value.toString());
  }
}
