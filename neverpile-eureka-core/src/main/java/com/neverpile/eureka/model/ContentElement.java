package com.neverpile.eureka.model;

import java.util.Objects;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class ContentElement extends EncryptableElement {
  private String id;

  private String fileName;

  private String role;

  @JsonSerialize(using = MediaTypeSerializer.class)
  @JsonDeserialize(using = MediaTypeDeserializer.class)
  private MediaType type;

  private long length;

  private Digest digest;

  public String getId() {
    return id;
  }

  public void setContentElementId(final String id) {
    this.id = id;
  }

  public String getRole() {
    return role;
  }

  public void setRole(final String role) {
    this.role = role;
  }

  @Pattern(regexp = "\\w+/\\w+")
  public MediaType getType() {
    return type;
  }

  public void setType(final MediaType type) {
    this.type = type;
  }

  @Min(-1)
  public long getLength() {
    return length;
  }

  public void setLength(final long length) {
    this.length = length;
  }

  public Digest getDigest() {
    return digest;
  }

  public void setDigest(final Digest digest) {
    this.digest = digest;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    ContentElement other = (ContentElement) obj;
    if (digest == null) {
      if (other.digest != null)
        return false;
    } else if (!digest.equals(other.digest))
      return false;
    if (fileName == null) {
      if (other.fileName != null)
        return false;
    } else if (!fileName.equals(other.fileName))
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (length != other.length)
      return false;
    if (role == null) {
      if (other.role != null)
        return false;
    } else if (!role.equals(other.role))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), id, fileName, role, type, length, digest);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContentElement {\n");

    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    encryption: ").append(toIndentedString(getEncryption())).append("\n");
    sb.append("    length: ").append(toIndentedString(length)).append("\n");
    sb.append("    digest: ").append(toIndentedString(digest)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(final java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(final String fileName) {
    this.fileName = fileName;
  }
}
