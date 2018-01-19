package com.cogniteev.cognisearch.event.model;

/**
 * Created by armel on 05/01/18.
 */
public class RessourcePojo {
  private String uri;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getLabels() {
    return labels;
  }

  public void setLabels(String labels) {
    this.labels = labels;
  }

  private String name;
  private String labels;
}
