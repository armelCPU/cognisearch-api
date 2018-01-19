package com.cogniteev.cognisearch.event.model;

/**
 * Created by armel on 05/01/18.
 */
public class ResultVenue {
  private String city;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String name;

  public Resultpoint getLocation() {
    return location;
  }

  public void setLocation(Resultpoint location) {
    this.location = location;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  private Resultpoint location;
}
