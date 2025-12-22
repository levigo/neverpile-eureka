package com.neverpile.eureka.security.oauth2.advice;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.neverpile.eureka.rest.api.document.DocumentDto;

@ControllerAdvice
@Order(100)
// FIXME: do we still need this, now that we have the authorization module? See NPE-39.
public class DocumentScopeAuthorization implements ResponseBodyAdvice<DocumentDto> {

  @Override
  public DocumentDto beforeBodyWrite(final DocumentDto body, final MethodParameter returnType, final MediaType selectedContentType,
      final Class<? extends HttpMessageConverter<?>> selectedConverterType, final ServerHttpRequest request,
      final ServerHttpResponse response) {
    // Get authentication.
    Authentication rawAuthentication = SecurityContextHolder.getContext().getAuthentication();
    OAuth2Authentication oAuth2Authentication;
    if (rawAuthentication instanceof OAuth2Authentication authentication) {
      oAuth2Authentication = authentication;
    } else {
      throw new IllegalStateException("Authentication not supported!");
    }
    // Check authorization.
    if (request.getURI().getPath().toString().startsWith("/api/v1/documents")) {
      if (!oAuth2Authentication.getOAuth2Request().getScope().stream().anyMatch(auth -> auth.equals("document"))) {
        throw new AccessDeniedException("No permission to scope \"document\".");
      }
    } else {
      if (!oAuth2Authentication.getOAuth2Request().getScope().stream().anyMatch(auth -> auth.equals("public"))) {
        throw new AccessDeniedException("No permission to scope \"public\".");
      }
    }
    return body;
  }

  @Override
  public boolean supports(final MethodParameter returnType, final Class<? extends HttpMessageConverter<?>> converterType) {
    return returnType.getParameterType() == DocumentDto.class;
  }
}
