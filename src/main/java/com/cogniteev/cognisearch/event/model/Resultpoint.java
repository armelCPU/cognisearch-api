package com.cogniteev.cognisearch.event.model;

/**
 * Created by armel on 05/01/18.
 */
public class Resultpoint {
  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLon() {
    return lon;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }

  private double lat;
  private double lon;
}
