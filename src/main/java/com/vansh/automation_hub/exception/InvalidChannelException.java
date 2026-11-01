package com.vansh.automation_hub.exception;

// Channel ya target galat/unavailable ho. "field" batata hai ki error kis field ka hai
public class InvalidChannelException extends RuntimeException {

  private final String field;

  public InvalidChannelException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}