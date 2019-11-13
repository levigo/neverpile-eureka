package com.neverpile.eureka.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.util.StringUtils;

import com.neverpile.eureka.model.ObjectName;

/**
 * A collection of utility methods related to {@link ObjectName}s.
 */
public class ObjectNames {

  private ObjectNames() {
  }

  /**
   * Escape the string using an encoding based on RFC 3986 but with more aggressive escaping of all
   * characters except alphanumerics '-' and '_'.
   * 
   * @param s the string to escape
   * @return the escaped string
   */
  public static String escape(final String s) {
    if (!StringUtils.hasLength(s)) {
      return s;
    }

    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
    boolean changed = false;
    for (byte b : bytes) {
      if (isAllowed(b & 0xff)) {
        bos.write(b);
      } else {
        bos.write('%');
        bos.write(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)));
        bos.write(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
        changed = true;
      }
    }

    return (changed ? new String(bos.toByteArray(), StandardCharsets.UTF_8) : s);
  }

  private static boolean isAllowed(final int c) {
    return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '-' || c == '_';
  }

  public static String unescape(final String s) {
    return StringUtils.uriDecode(s, StandardCharsets.UTF_8);
  }
}
