package com.neverpile.eureka.security.oauth2.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

import com.neverpile.eureka.security.oauth2.advice.DocumentScopeAuthorization;

@AutoConfiguration
@Import({ResourceServerConfiguration.class, AuthorizationServerConfiguration.class, DocumentScopeAuthorization.class})
public class NeverpileEurekaOAuth2AutoConfiguration {
}
