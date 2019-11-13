package com.neverpile.eureka.model;

import java.util.Objects;



public abstract class EncryptableElement {

  private EncryptionType encryption;
  
  private String keyHint;

  public EncryptableElement() {
    super();
  }

  public EncryptionType getEncryption() {
    return encryption;
  }

  public void setEncryption(final EncryptionType encryption) {
    this.encryption = encryption;
  }

  public String getKeyHint() {
    return keyHint;
  }

  public void setKeyHint(final String keyHint) {
    this.keyHint = keyHint;
  }

  @Override
  public int hashCode() {
    return Objects.hash(encryption, keyHint);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    EncryptableElement other = (EncryptableElement) obj;
    if (encryption != other.encryption)
      return false;
    if (keyHint == null) {
      if (other.keyHint != null)
        return false;
    } else if (!keyHint.equals(other.keyHint))
      return false;
    return true;
  }
}