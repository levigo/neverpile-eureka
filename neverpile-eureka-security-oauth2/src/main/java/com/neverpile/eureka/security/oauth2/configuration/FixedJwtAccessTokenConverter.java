package com.neverpile.eureka.security.oauth2.configuration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;


public class FixedJwtAccessTokenConverter extends JwtAccessTokenConverter {

  @Override
  public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
    Map<String, String> parameters = new HashMap<String, String>();
    Set<String> scope = extractScope(map);
    Authentication user = new DefaultUserAuthenticationConverter().extractAuthentication(map);
    String clientId = (String) map.get(CLIENT_ID);
    parameters.put(CLIENT_ID, clientId);
    if (map.containsKey(GRANT_TYPE)) {
      parameters.put(GRANT_TYPE, (String) map.get(GRANT_TYPE));
    }
    Set<String> resourceIds = new LinkedHashSet<String>(map.containsKey(AUD) ? getAudience(map)
        : Collections.<String>emptySet());

    Collection<? extends GrantedAuthority> authorities = null;
    if (user==null && map.containsKey(AUTHORITIES)) {
          try {
            // DEFAULT
            @SuppressWarnings("unchecked") String[] roles = ((Collection<String>) map.get(AUTHORITIES)).toArray(new String[0]);
            authorities = AuthorityUtils.createAuthorityList(roles);
          }catch(Exception e) {
            // FIX
            String role = (String) map.get(AUTHORITIES);
            authorities = AuthorityUtils.createAuthorityList(role);
          }
    }
    OAuth2Request request = new OAuth2Request(parameters, clientId, authorities, true, scope, resourceIds, null, null,
        null);
    return new OAuth2Authentication(request, user);
  }

  private Set<String> extractScope(Map<String, ?> map) {
    Set<String> scope = Collections.emptySet();
    if (map.containsKey(SCOPE)) {
      Object scopeObj = map.get(SCOPE);
      if (String.class.isInstance(scopeObj)) {
        scope = new LinkedHashSet<String>(Arrays.asList(String.class.cast(scopeObj).split(" ")));
      } else if (Collection.class.isAssignableFrom(scopeObj.getClass())) {
        @SuppressWarnings("unchecked")
        Collection<String> scopeColl = (Collection<String>) scopeObj;
        scope = new LinkedHashSet<String>(scopeColl);	// Preserve ordering
      }
    }
    return scope;
  }

  private Collection<String> getAudience(Map<String, ?> map) {
    Object auds = map.get(AUD);
    if (auds instanceof Collection) {
      @SuppressWarnings("unchecked")
      Collection<String> result = (Collection<String>) auds;
      return result;
    }
    return Collections.singleton((String)auds);
  }
}
