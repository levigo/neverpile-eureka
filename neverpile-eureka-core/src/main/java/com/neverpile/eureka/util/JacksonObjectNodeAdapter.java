package com.neverpile.eureka.util;

import javax.annotation.PostConstruct;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class JacksonObjectNodeAdapter extends XmlAdapter<String, ObjectNode> {

  private static ObjectMapper defaultObjectMapper;

  private static ObjectMapper getObjectMapper() {
    if(null == defaultObjectMapper)
      defaultObjectMapper = new ObjectMapper();
    
    return defaultObjectMapper;
  }
  
  @Autowired
  private ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    defaultObjectMapper = objectMapper;
  }

  @Override
  public ObjectNode unmarshal(final String v) throws Exception {
   return (ObjectNode) getObjectMapper().readTree(v);
  }

  @Override
  public String marshal(final ObjectNode v) throws Exception {
    return getObjectMapper().writeValueAsString(v);
  }

}
