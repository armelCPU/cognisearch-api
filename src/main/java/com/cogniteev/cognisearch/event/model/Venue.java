package com.cogniteev.cognisearch.event.model;

import org.elasticsearch.common.geo.GeoPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by armel on 07/12/16.
 */
public class Venue {
  private String addSupplement = null;
  private String streetNumber = null;
  private String streetName = null;
  private String zipCode = null;

  public String getCouncilName() {
    return councilName;
  }

  public void setCouncilName(String councilName) {
    this.councilName = councilName;
  }

  public String getZipCode() {
    return zipCode;
  }

  public void setZipCode(String zipCode) {
    this.zipCode = zipCode;
  }

  public String getStreetName() {
    return streetName;
  }

  public void setStreetName(String streetName) {
    this.streetName = streetName;
  }

  public String getStreetNumber() {
    return streetNumber;
  }

  public void setStreetNumber(String streetNumber) {
    this.streetNumber = streetNumber;
  }

  public String getAddSupplement() {
    return addSupplement;
  }

  public void setAddSupplement(String addSupplement) {
    this.addSupplement = addSupplement;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }


  private String councilName = null;
  private String country = null;
  private String addressString = null;

  public GeoPoint getPoint() {
    return point;
  }

  public void setPoint(GeoPoint point) {
    this.point = point;
  }

  public List<GeoPoint> getPolygon() {
    return polygon;
  }

  public void setPolygon(List<GeoPoint> polygon) {
    this.polygon = polygon;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getAddressString() {
    return addressString;
  }

  public void setAddressString(String addressString) {
    this.addressString = addressString;
  }

  private GeoPoint point = null;
  private List<GeoPoint> polygon = null;
  private String uri = null;

  /**
   * To String of a venue
   * @return
   */
  public String toString(){
    String res = "";
    if(addSupplement != null)
      res += "Complement : " + (addSupplement + "\n");
    if(streetNumber != null)
      res += "Numero: " + (streetNumber + "\n");
    if(streetName != null)
      res += "Voie : " + (streetName + "\n");
    if(zipCode != null)
      res += "CP : " + (zipCode + "\n");
    if(councilName != null)
      res += "Commune : " + (councilName + "\n");
    if(country != null)
      res += "Pays : " + (country + "\n");
    if (point != null)
      res += "Point : "  + point + "\n";
    if (addressString != null)
      res += "Adresse : " + addressString + "\n";


    return res;
  }

  /**
   * Build the complete address
   * @return
   */
  public String concatenate(){
    String res = "";
    /*if(addSupplement != null)
      res += (addSupplement + " ");*/
    if(streetNumber != null)
      res += (streetNumber + " ");
    if(streetName != null)
      res += (streetName + " ");
    if(zipCode != null)
      res += (zipCode + " ");
    if(councilName != null)
      res += (councilName + " ");

    return res;
  }

  /**
   * This function converts a Venue Object to a map
   * @return
   */
  public Map<String, Object> toMap()
  {
    Map<String, Object> venue = new HashMap<>();

    if(addSupplement != null)
      venue.put("addSupplement", addSupplement);

    if(streetNumber != null)
      venue.put("streetNumber", streetNumber);

    if(streetName != null)
      venue.put("streetName", streetName);

    if(zipCode != null)
      venue.put("zipCode", zipCode);

    if(councilName != null)
      venue.put("councilName", councilName);

    if(country != null)
      venue.put("country", country);

    if (uri != null)
      venue.put("uri", uri);

    if (point != null)
      venue.put("point", point);

    if (polygon != null)
      venue.put("polygon", polygon);

    if (addressString != null)
      venue.put("addressString", addressString);

    return  venue;
  }

  /**
   * Spatial proximity of places
   * @param v
   * @return
   */
  public boolean equalsVenue(Venue v) {
    if (this == null || v == null)
      return false;

    return ( Math.abs(this.getPoint().getLat() - v.getPoint().getLat()) < 0.0001 && Math.abs(this.getPoint().getLon() - v.getPoint().getLon()) < 0.0001);

  }
}
