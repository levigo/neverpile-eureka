package com.neverpile.eureka.security.oauth2.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

@Configuration
@ConditionalOnProperty(name = "neverpile-eureka.oauth2.embedded-authorization-server.enabled", havingValue = "true", matchIfMissing = false)
@EnableAuthorizationServer
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {

  @Value("${resource.id:spring-boot-application}")
  private String resourceId;

  @Autowired
  AuthenticationConfiguration authenticationConfiguration;

  // No default - require explicit setting
  @Value("${neverpile-eureka.oauth2.embedded-authorization-server.key}")
  private final String KEY = "CHANGE_ME_KEY";

  @Override
  public void configure(final AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
    TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
    tokenEnhancerChain.setTokenEnhancers(Arrays.asList(tokenEnhancer(), accessTokenConverter()));
    endpoints.authenticationManager(authenticationConfiguration.getAuthenticationManager()).tokenStore(
        tokenStore()).tokenEnhancer(tokenEnhancerChain);
  }

  @Override
  public void configure(final AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
    oauthServer //
        .tokenKeyAccess("isAnonymous() || hasAuthority('ROLE_TRUSTED_CLIENT')") //
        .checkTokenAccess("hasAuthority('ROLE_TRUSTED_CLIENT')");
  }

  // FIXME: is there an option for file-based configuration?
  @Override
  public void configure(final ClientDetailsServiceConfigurer clients) throws Exception {
    // @formatter:off
    clients.inMemory()
      .withClient("normal-app")
        .authorizedGrantTypes("authorization_code", "implicit")
        .authorities("ROLE_CLIENT")
        .scopes("document", "public")
        .resourceIds(resourceId)
        .and()
      .withClient("trusted-app")
        .authorizedGrantTypes("client_credentials", "password")
        .authorities("ROLE_TRUSTED_CLIENT")
        .scopes("document", "public")
        .resourceIds(resourceId)
        .secret("{noop}secret");
    // @formatter:on
  }

  @Bean
  public TokenStore tokenStore() {
    return new JwtTokenStore(accessTokenConverter());
  }

  @Bean
  public FixedJwtAccessTokenConverter accessTokenConverter() {
    FixedJwtAccessTokenConverter converter = new FixedJwtAccessTokenConverter();
    converter.setSigningKey(KEY);
    return converter;
  }

  @Bean
  @Primary
  public DefaultTokenServices tokenServices() {
    DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
    defaultTokenServices.setTokenStore(tokenStore());
    defaultTokenServices.setSupportRefreshToken(true);
    defaultTokenServices.setTokenEnhancer(tokenEnhancer());
    return defaultTokenServices;
  }

  @Bean
  public TokenEnhancer tokenEnhancer() {
    return new CustomTokenEnhancer();
  }

  class CustomTokenEnhancer implements TokenEnhancer {
    @Override
    public OAuth2AccessToken enhance(final OAuth2AccessToken accessToken, final OAuth2Authentication authentication) {
      Map<String, Object> additionalInfo = new HashMap<>();
      ArrayList<String> authArray = new ArrayList<>();
      for (GrantedAuthority ga : authentication.getAuthorities()) {
        authArray.add(ga.getAuthority());
      }

      additionalInfo.put("authorities", String.join(" ", authArray));
      additionalInfo.put("username", authentication.getName());
      additionalInfo.put("clientId", authentication.getOAuth2Request().getClientId());
      ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
      return accessToken;
    }
  }
}