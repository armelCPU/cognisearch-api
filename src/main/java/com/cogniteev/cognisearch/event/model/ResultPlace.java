package com.cogniteev.cognisearch.event.model;

import java.util.List;

/**
 * Created by armel on 13/01/18.
 */
public class ResultPlace {
  public List<Resultpoint> getPoints() {
    return points;
  }

  public void setPoints(List<Resultpoint> points) {
    this.points = points;
  }

  private List<Resultpoint> points;

  private int id;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  private String name;
  private String uri;
}
