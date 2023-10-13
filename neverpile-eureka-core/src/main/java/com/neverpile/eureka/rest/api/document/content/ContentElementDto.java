package com.neverpile.eureka.rest.api.document.content;

import java.util.Objects;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.core.MediaType;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.neverpile.eureka.model.Digest;
import com.neverpile.eureka.model.EncryptionType;
import com.neverpile.eureka.model.MediaTypeDeserializer;
import com.neverpile.eureka.model.MediaTypeSerializer;
import com.neverpile.eureka.rest.api.document.IDto;



public class ContentElementDto extends RepresentationModel<ContentElementDto> implements IDto {
  private String contentElementId;
  
  private String fileName;
  
  private String role;
  
  @JsonSerialize(using=MediaTypeSerializer.class)
  @JsonDeserialize(using=MediaTypeDeserializer.class)
  private MediaType type;

  private EncryptionType encryption;
  
  private long length;

  private Digest digest;

  @JsonProperty("id")
  public String getContentElementId() {
    return contentElementId;
  }

  public void setContentElementId(final String id) {
    this.contentElementId = id;
  }

  public String getRole() {
    return role;
  }

  public void setRole(final String role) {
    this.role = role;
  }

  @Pattern(regexp = "[-\\w+]+/[-\\w+]+")
  public MediaType getType() {
    return type;
  }

  public void setType(final MediaType type) {
    this.type = type;
  }

  public EncryptionType getEncryption() {
    return encryption;
  }

  public void setEncryption(final EncryptionType encryption) {
    this.encryption = encryption;
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
  public boolean equals(final java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContentElementDto contentElement = (ContentElementDto) o;
    return Objects.equals(contentElementId, contentElement.contentElementId)
        && Objects.equals(role, contentElement.role) && Objects.equals(type, contentElement.type)
        && Objects.equals(encryption, contentElement.encryption) && Objects.equals(length, contentElement.length)
        && Objects.equals(digest, contentElement.digest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentElementId, role, type, encryption, length, digest);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContentElement {\n");

    sb.append("    id: ").append(toIndentedString(contentElementId)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    encryption: ").append(toIndentedString(encryption)).append("\n");
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
