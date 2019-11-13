package com.neverpile.eureka.security.oauth2.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.neverpile.eureka.security.oauth2.advice.DocumentScopeAuthorization;

@Configuration
@Import({ResourceServerConfiguration.class, AuthorizationServerConfiguration.class, DocumentScopeAuthorization.class})
public class NeverpileEurekaOAuth2AutoConfiguration {
}
