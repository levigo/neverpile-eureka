package com.neverpile.eureka.security.oauth2.configuration;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableResourceServer
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
  @Value("${resource.id:spring-boot-application}")
  private String resourceId;

  // FIXME: What happens, if the AuthorizaionServerConfiguration isn't active?
  @Autowired
  private DefaultTokenServices tokenServices;

  @Autowired
  private TokenStore tokenStore;

  @Override
  public void configure(final ResourceServerSecurityConfigurer resources) {
    resources.resourceId(resourceId).tokenServices(tokenServices).tokenStore(tokenStore);
  }

  @Override
  public void configure(final HttpSecurity http) throws Exception {
    http.securityMatcher(new OAuthRequestedMatcher()).authorizeRequests().requestMatchers(
        HttpMethod.OPTIONS).permitAll().anyRequest().authenticated();
  }

  private static class OAuthRequestedMatcher implements RequestMatcher {
    @Override
    public boolean matches(final HttpServletRequest request) {
      String auth = request.getHeader("Authorization");
      // Determine if the client request contained an OAuth Authorization
      boolean haveOauth2Token = (auth != null) && auth.startsWith("Bearer");
      boolean haveAccessToken = request.getParameter("access_token") != null;
      return haveOauth2Token || haveAccessToken;
    }
  }
}