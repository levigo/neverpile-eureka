/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.neverpile.eureka.rest.api.document.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;

/**
 * A variant of the {@link RequestParamMethodArgumentResolver} that supports the injection of all
 * request parts in the following cases:
 * <ul>
 * <li>{@link java.util.Collection Collection&lt;MultipartFile&gt;}
 * <li>{@link java.util.List List&lt;MultipartFile&gt;}
 * </ul>
 */
public class AllRequestPartsMethodArgumentResolver implements HandlerMethodArgumentResolver {
  public static class AllRequestParts {
    public AllRequestParts(final List<MultipartFile> allParts) {
      this.allParts = Collections.unmodifiableList(allParts);
    }

    private final List<MultipartFile> allParts;
    
    public List<MultipartFile> getAllParts() {
      return allParts;
    }
  }

  private static class SimpleMultipartFile implements MultipartFile {

    private final Part part;

    private final String filename;

    public SimpleMultipartFile(final Part part, final String filename) {
      this.part = part;
      this.filename = filename;
    }

    @Override
    public String getName() {
      return this.part.getName();
    }

    @Override
    public String getOriginalFilename() {
      return this.filename;
    }

    @Override
    public String getContentType() {
      return this.part.getContentType();
    }

    @Override
    public boolean isEmpty() {
      return (this.part.getSize() == 0);
    }

    @Override
    public long getSize() {
      return this.part.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
      return FileCopyUtils.copyToByteArray(this.part.getInputStream());
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return this.part.getInputStream();
    }

    @Override
    public void transferTo(final File dest) throws IOException, IllegalStateException {
      this.part.write(dest.getPath());
      if (dest.isAbsolute() && !dest.exists()) {
        FileCopyUtils.copy(this.part.getInputStream(), Files.newOutputStream(dest.toPath()));
      }
    }
  }

  /**
   * Whether the given {@linkplain MethodParameter method parameter} is a multi-part supported.
   * Supports the following:
   * <ul>
   * <li>annotated with {@code @RequestPart}
   * <li>of type {@link MultipartFile} unless annotated with {@code @RequestParam}
   * <li>of type {@code javax.servlet.http.Part} unless annotated with {@code @RequestParam}
   * </ul>
   */
  @Override
  public boolean supportsParameter(final MethodParameter parameter) {
    return AllRequestParts.class.isAssignableFrom(parameter.getParameterType());
  }

  @Nullable
  private static Class<?> getCollectionParameterType(final MethodParameter methodParam) {
    Class<?> paramType = methodParam.getNestedParameterType();
    if (Collection.class == paramType || List.class.isAssignableFrom(paramType)) {
      Class<?> valueType = ResolvableType.forMethodParameter(methodParam).asCollection().resolveGeneric();
      if (valueType != null) {
        return valueType;
      }
    }
    return null;
  }

  private List<MultipartFile> parseRequest(final HttpServletRequest request) throws IOException, ServletException {
    Collection<Part> parts = request.getParts();
    List<MultipartFile> files = new ArrayList<>();
    for (Part part : parts) {
      String headerValue = part.getHeader(HttpHeaders.CONTENT_DISPOSITION);
      ContentDisposition disposition = ContentDisposition.parse(headerValue);
      String filename = disposition.getFilename();
      files.add(new SimpleMultipartFile(part, filename));
    }
    return files;
  }

  @Override
  @Nullable
  public Object resolveArgument(final MethodParameter parameter, @Nullable final ModelAndViewContainer mavContainer,
      final NativeWebRequest request, @Nullable final WebDataBinderFactory binderFactory) throws Exception {

    HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
    Assert.state(servletRequest != null, "No HttpServletRequest");

    List<MultipartFile> allParts = parseRequest(servletRequest);
    
    return new AllRequestParts(allParts);
  }
}
