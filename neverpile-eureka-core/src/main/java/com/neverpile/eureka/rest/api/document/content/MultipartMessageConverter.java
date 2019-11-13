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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;

/**
 * Implementation of {@link HttpMessageConverter} to write any MIME multipart format. Ideally,
 * spring would support this out of the box, and {@link FormHttpMessageConverter} is pretty close.
 * However, it cannot easily be coerced to support anything besides the media types
 * {@code "application/x-www-form-urlencoded"} and {@code "multipart/form-data"}. This
 * implementation is, however, massively based on the it.
 *
 * @see FormHttpMessageConverter
 */
public class MultipartMessageConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

  public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private List<MediaType> excludedMediaTypes = new ArrayList<>();

  private List<HttpMessageConverter<?>> partConverters = new ArrayList<>();

  private Charset charset = DEFAULT_CHARSET;

  @Nullable
  private Charset multipartCharset;


  public MultipartMessageConverter() {
    this.excludedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
    this.excludedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);

    StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
    stringHttpMessageConverter.setWriteAcceptCharset(false); // see SPR-7316

    this.partConverters.add(new ByteArrayHttpMessageConverter());
    this.partConverters.add(stringHttpMessageConverter);
    this.partConverters.add(new ResourceHttpMessageConverter());

    applyDefaultCharset();
  }


  /**
   * Set the list of {@link MediaType} objects <em>not</em> by this converter.
   * 
   * @param excludedMediaTypes the media types to exclude
   */
  public void setExcludedMediaTypes(final List<MediaType> excludedMediaTypes) {
    this.excludedMediaTypes = excludedMediaTypes;
  }

  public List<MediaType> getExcludedMediaTypes() {
    return Collections.unmodifiableList(this.excludedMediaTypes);
  }

  /**
   * Set the message body converters to use. These converters are used to convert objects to MIME
   * parts.
   * 
   * @param partConverters the part converters to use
   */
  public void setPartConverters(final List<HttpMessageConverter<?>> partConverters) {
    Assert.notEmpty(partConverters, "'partConverters' must not be empty");
    this.partConverters = partConverters;
  }

  /**
   * Add a message body converter. Such a converter is used to convert objects to MIME parts.
   * 
   * @param partConverter the part converter to add
   */
  public void addPartConverter(final HttpMessageConverter<?> partConverter) {
    Assert.notNull(partConverter, "'partConverter' must not be null");
    this.partConverters.add(partConverter);
  }

  /**
   * Set the default character set to use for reading and writing form data when the request or
   * response Content-Type header does not explicitly specify it.
   * <p>
   * As of 4.3, this is also used as the default charset for the conversion of text bodies in a
   * multipart request.
   * <p>
   * As of 5.0 this is also used for part headers including "Content-Disposition" (and its filename
   * parameter) unless (the mutually exclusive) {@link #setMultipartCharset} is also set, in which
   * case part headers are encoded as ASCII and <i>filename</i> is encoded with the "encoded-word"
   * syntax from RFC 2047.
   * <p>
   * By default this is set to "UTF-8".
   * 
   * @param charset default character set to use
   */
  public void setCharset(@Nullable final Charset charset) {
    if (charset != this.charset) {
      this.charset = (charset != null ? charset : DEFAULT_CHARSET);
      applyDefaultCharset();
    }
  }

  /**
   * Apply the configured charset as a default to registered part converters.
   */
  private void applyDefaultCharset() {
    for (HttpMessageConverter<?> candidate : this.partConverters) {
      if (candidate instanceof AbstractHttpMessageConverter) {
        AbstractHttpMessageConverter<?> converter = (AbstractHttpMessageConverter<?>) candidate;
        // Only override default charset if the converter operates with a charset to begin with...
        if (converter.getDefaultCharset() != null) {
          converter.setDefaultCharset(this.charset);
        }
      }
    }
  }

  /**
   * Set the character set to use when writing multipart data to encode file names. Encoding is
   * based on the "encoded-word" syntax defined in RFC 2047 and relies on {@code MimeUtility} from
   * "javax.mail".
   * <p>
   * As of 5.0 by default part headers, including Content-Disposition (and its filename parameter)
   * will be encoded based on the setting of {@link #setCharset(Charset)} or {@code UTF-8} by
   * default.
   * 
   * @param charset the character set to use when writing multipart data
   * 
   * @since 4.1.1
   * @see <a href="http://en.wikipedia.org/wiki/MIME#Encoded-Word">Encoded-Word</a>
   */
  public void setMultipartCharset(final Charset charset) {
    this.multipartCharset = charset;
  }


  @Override
  public boolean canRead(final Class<?> clazz, @Nullable final MediaType mediaType) {
    return false;
  }

  @Override
  public boolean canWrite(final Class<?> clazz, @Nullable final MediaType mediaType) {
    if (!MultiValueMap.class.isAssignableFrom(clazz)) {
      return false;
    }
    if (mediaType == null || !mediaType.getType().equals("multipart")) {
      return false;
    }
    for (MediaType excludedMediaType : getExcludedMediaTypes()) {
      if (excludedMediaType.isCompatibleWith(mediaType)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public MultiValueMap<String, String> read(@Nullable final Class<? extends MultiValueMap<String, ?>> clazz,
      final HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
    throw new UnsupportedOperationException();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void write(final MultiValueMap<String, ?> map, @Nullable final MediaType contentType,
      final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
    final MultiValueMap<String, Object> parts = (MultiValueMap<String, Object>) map;
    final byte[] boundary = generateMultipartBoundary();
    Map<String, String> parameters = new LinkedHashMap<>(2);
    if (!isFilenameCharsetSet()) {
      parameters.put("charset", this.charset.name());
    }
    parameters.put("boundary", new String(boundary, "US-ASCII"));

    MediaType parameterizedContentType = new MediaType(contentType, parameters);
    HttpHeaders headers = outputMessage.getHeaders();
    headers.setContentType(parameterizedContentType);

    if (outputMessage instanceof StreamingHttpOutputMessage) {
      StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
      streamingOutputMessage.setBody(outputStream -> {
        writeParts(outputStream, parts, boundary);
        writeEnd(outputStream, boundary);
      });
    } else {
      writeParts(outputMessage.getBody(), parts, boundary);
      writeEnd(outputMessage.getBody(), boundary);
    }
  }

  /**
   * When {@link #setMultipartCharset(Charset)} is configured (i.e. RFC 2047, "encoded-word" syntax)
   * we need to use ASCII for part headers or otherwise we encode directly using the configured
   * {@link #setCharset(Charset)}.
   */
  private boolean isFilenameCharsetSet() {
    return (this.multipartCharset != null);
  }

  private void writeParts(final OutputStream os, final MultiValueMap<String, Object> parts, final byte[] boundary)
      throws IOException {
    for (Map.Entry<String, List<Object>> entry : parts.entrySet()) {
      String name = entry.getKey();
      for (Object part : entry.getValue()) {
        if (part != null) {
          writeBoundary(os, boundary);
          writePart(name, getHttpEntity(part), os);
          writeNewLine(os);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void writePart(final String name, final HttpEntity<?> partEntity, final OutputStream os) throws IOException {
    Object partBody = partEntity.getBody();
    if (partBody == null) {
      throw new IllegalStateException("Empty body for part '" + name + "': " + partEntity);
    }
    Class<?> partType = partBody.getClass();
    HttpHeaders partHeaders = partEntity.getHeaders();
    MediaType partContentType = partHeaders.getContentType();
    for (HttpMessageConverter<?> messageConverter : this.partConverters) {
      if (messageConverter.canWrite(partType, partContentType)) {
        Charset charset = isFilenameCharsetSet() ? StandardCharsets.US_ASCII : this.charset;
        HttpOutputMessage multipartMessage = new MultipartHttpOutputMessage(os, charset);
        multipartMessage.getHeaders().setContentDispositionFormData(name, getFilename(partBody));
        if (!partHeaders.isEmpty()) {
          multipartMessage.getHeaders().putAll(partHeaders);
        }
        ((HttpMessageConverter<Object>) messageConverter).write(partBody, partContentType, multipartMessage);
        return;
      }
    }
    throw new HttpMessageNotWritableException("Could not write request: no suitable HttpMessageConverter "
        + "found for request type [" + partType.getName() + "]");
  }

  /**
   * Generate a multipart boundary.
   * <p>
   * This implementation delegates to {@link MimeTypeUtils#generateMultipartBoundary()}.
   * 
   * @return the message boundary pattern
   */
  protected byte[] generateMultipartBoundary() {
    return MimeTypeUtils.generateMultipartBoundary();
  }

  /**
   * Return an {@link HttpEntity} for the given part Object.
   * 
   * @param part the part to return an {@link HttpEntity} for
   * @return the part Object itself it is an {@link HttpEntity}, or a newly built {@link HttpEntity}
   *         wrapper for that part
   */
  protected HttpEntity<?> getHttpEntity(final Object part) {
    return (part instanceof HttpEntity ? (HttpEntity<?>) part : new HttpEntity<>(part));
  }

  /**
   * Return the filename of the given multipart part. This value will be used for the
   * {@code Content-Disposition} header.
   * <p>
   * The default implementation returns {@link Resource#getFilename()} if the part is a
   * {@code Resource}, and {@code null} in other cases. Can be overridden in subclasses.
   * 
   * @param part the part to determine the file name for
   * @return the filename, or {@code null} if not known
   */
  @Nullable
  protected String getFilename(final Object part) {
    if (part instanceof Resource) {
      Resource resource = (Resource) part;
      return resource.getFilename();
    } else {
      return null;
    }
  }


  private void writeBoundary(final OutputStream os, final byte[] boundary) throws IOException {
    os.write('-');
    os.write('-');
    os.write(boundary);
    writeNewLine(os);
  }

  private static void writeEnd(final OutputStream os, final byte[] boundary) throws IOException {
    os.write('-');
    os.write('-');
    os.write(boundary);
    os.write('-');
    os.write('-');
    writeNewLine(os);
  }

  private static void writeNewLine(final OutputStream os) throws IOException {
    os.write('\r');
    os.write('\n');
  }


  /**
   * Implementation of {@link org.springframework.http.HttpOutputMessage} used to write a MIME
   * multipart.
   */
  private static class MultipartHttpOutputMessage implements HttpOutputMessage {

    private final OutputStream outputStream;

    private final Charset charset;

    private final HttpHeaders headers = new HttpHeaders();

    private boolean headersWritten = false;

    public MultipartHttpOutputMessage(final OutputStream outputStream, final Charset charset) {
      this.outputStream = outputStream;
      this.charset = charset;
    }

    @Override
    public HttpHeaders getHeaders() {
      return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
    }

    @Override
    public OutputStream getBody() throws IOException {
      writeHeaders();
      return this.outputStream;
    }

    private void writeHeaders() throws IOException {
      if (!this.headersWritten) {
        for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
          byte[] headerName = getBytes(entry.getKey());
          for (String headerValueString : entry.getValue()) {
            byte[] headerValue = getBytes(headerValueString);
            this.outputStream.write(headerName);
            this.outputStream.write(':');
            this.outputStream.write(' ');
            this.outputStream.write(headerValue);
            writeNewLine(this.outputStream);
          }
        }
        writeNewLine(this.outputStream);
        this.headersWritten = true;
      }
    }

    private byte[] getBytes(final String name) {
      return name.getBytes(this.charset);
    }
  }

  @Override
  public List<MediaType> getSupportedMediaTypes() {
    return Collections.singletonList(MediaType.valueOf("multipart/*"));
  }
}
