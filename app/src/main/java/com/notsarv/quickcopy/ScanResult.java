package com.notsarv.quickcopy;

public class ScanResult {
  public long id;
  public String imagePath;
  public String text;
  public long timestamp;

  public ScanResult(long id, String imagePath, String text, long timestamp) {
    this.id = id;
    this.imagePath = imagePath;
    this.text = text;
    this.timestamp = timestamp;
  }
}
