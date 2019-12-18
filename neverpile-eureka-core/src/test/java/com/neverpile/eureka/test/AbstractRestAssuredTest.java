package com.neverpile.eureka.test;

import java.lang.reflect.Type;
import java.time.Instant;

import org.junit.Before;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neverpile.eureka.model.Document;

import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.specification.RequestSpecification;


public abstract class AbstractRestAssuredTest {
  /**
   * A JacksonMixIn used to disable sending entity links. In theory, it should suffice to use the
   * {@link ObjectMapper} provided by the app context for {@link RestAssured}, but with a recent
   * update, spring HATEOAS initialization is broken in a fancy new way which prevents this.
   * <p>
   * FIXME: reconsider HATEOAS support...
   */
  @JsonIgnoreProperties("_links")
  private static abstract class ResourceSupportMixin extends RepresentationModel<ResourceSupportMixin> {
    @Override
    @JsonIgnore
    public abstract Links getLinks();
  }

  protected static final String D = "aTestDocument";

  @LocalServerPort
  int port;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected ModelMapper modelMapper;

  @Before
  public void setupRestAssured() {
    RestAssured.port = port;
  }

  @Before
  public void before() {
    // Make RestAssured use the application's object mapper so that support for
    // HATEOAS is available.
    RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
        new ObjectMapperConfig().jackson2ObjectMapperFactory(new Jackson2ObjectMapperFactory() {
          @Override
          public ObjectMapper create(final Type cls, final String charset) {
            return objectMapper.addMixIn(RepresentationModel.class, ResourceSupportMixin.class);
          }
        }));
  }

  protected Document createTestDocument() {
    Document doc = new Document();

    doc.setDateCreated(Instant.now());
    doc.setDateModified(Instant.now());

    return doc;
  }
  
  protected RequestSpecification givenVanillaCall() {
    return RestAssured //
        .given() //
        .accept(ContentType.JSON) //
        .contentType(ContentType.JSON) //
        .auth().preemptive().basic("user", "password");
  }
}