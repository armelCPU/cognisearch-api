package com.cogniteev.cognisearch.event.model;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by armel on 19/01/17.
 */
public class Place {

  private int id;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String name;

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

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

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  private String address;
  private double lat;
  private double lon;

  public Place(ResultSet rs) throws SQLException {
    if ( rs != null && !rs.wasNull()){
      id = rs.getInt("id");
      name = rs.getString("name");
      address = rs.getString("address");
      lat = rs.getDouble("lat");
      lon = rs.getDouble("lon");
    }
  }

  public Place(String name, String address, double lat, double lon) {
    this.name = name;
    this.address = address;
    this.lat = lat;
    this.lon = lon;
  }

  public Place() {

  }

  public String toString(){
    return name + "\n" +
           address + "\n" +
           lon + "\t" + lat + "\n";
  }
}
