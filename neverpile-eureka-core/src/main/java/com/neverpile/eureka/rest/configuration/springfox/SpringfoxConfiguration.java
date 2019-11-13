package com.neverpile.eureka.rest.configuration.springfox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.classmate.TypeResolver;
import com.neverpile.eureka.model.Digest;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.OAuthBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.schema.AlternateTypeRules;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.ClientCredentialsGrant;
import springfox.documentation.service.Contact;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger.web.SecurityConfigurationBuilder;

@Configuration
public class SpringfoxConfiguration  {
  private final String CLIENT_ID = "trusted-app";
  private final String CLIENT_SECRET = "secret";

  @Bean
  @ConditionalOnBean(TypeResolver.class)
  public Docket swaggerSpringMvcPlugin(final TypeResolver typeResolver) {
	  // @formatter:off
    return new Docket(DocumentationType.SWAGGER_2)
        .useDefaultResponseMessages(false)
        .apiInfo(apiInfo())
        .select()
        .paths(PathSelectors.regex("/api/.*"))
        .build()
        .securitySchemes(Collections.singletonList(oauth()))
        .tags( //
            new Tag("Document", "Document related APIs", 1), //
            new Tag("Content", "Document content element related APIs", 2),
            new Tag("Metadata", "Document metadata related APIs", 3),
            new Tag("Audit", "Audit-log related APIs", 4)
        )
        .additionalModels(typeResolver.resolve(Digest.class))
        .alternateTypeRules(AlternateTypeRules.newRule(typeResolver.resolve(MediaType.class),
            typeResolver.resolve(String.class)))
        ;
    // @formatter:on
  }

  @Bean
  SecurityScheme oauth() {
    return new OAuthBuilder()
        .name("oauth")
        .scopes(scopes())
        .grantTypes(grantTypes())
        .build();
  }

  @Bean
  List<GrantType> grantTypes() {
    List<GrantType> grantTypes = new ArrayList<>();
    grantTypes.add(new ClientCredentialsGrant("/oauth/token"));
    return grantTypes;
  }

  private List<AuthorizationScope> scopes() {
    ArrayList<AuthorizationScope> list = new ArrayList<>();
    list.add(new AuthorizationScope("public", "for public operations"));
    list.add(new AuthorizationScope("document", "for document operations"));
    return list;
  }

  @Bean
  public SecurityConfiguration securityInfo() {
    return SecurityConfigurationBuilder.builder()
        .clientId(CLIENT_ID)
        .clientSecret(CLIENT_SECRET)
        .scopeSeparator(" ")
        .useBasicAuthenticationWithAccessCodeGrant(true)
        .build();
  }

  private ApiInfo apiInfo() {
    // @formatter:off
    return new ApiInfoBuilder()
        .title("Neverpile eureka API")
        .description("")
        .contact(new Contact("levigo solutions gmbh", "https://levigo.de", "solutions@levigo.de"))
        .build();
    // @formatter:on
  }
}
