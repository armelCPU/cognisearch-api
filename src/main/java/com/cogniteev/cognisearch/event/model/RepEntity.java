package com.cogniteev.cognisearch.event.model;

import org.elasticsearch.common.geo.GeoPoint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by armel on 26/07/17.
 */
public class RepEntity {

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  private String id;

  private SimpleDate date = null;

  public Object getVenue() {
    return venue;
  }

  public void setVenue(Object venue) {
    this.venue = venue;
  }

  public SimpleDate getDate() {
    return date;
  }

  public void setDate(SimpleDate date) {
    this.date = date;
  }

  public SimpleHour getHour() {
    return hour;
  }

  public void setHour(SimpleHour hour) {
    this.hour = hour;
  }

  private Object venue = null;
  private SimpleHour hour = null;

  public RepEntity() {

  }

  public RepEntity(SimpleDate date, SimpleHour hour, Object venue){
    this.date = date;
    this.venue = venue;
    this.hour = hour;
  }

  public RepEntity(Map<String, Object> dic, String id){
    this.id =id;

    if ( dic.containsKey("venue")){
      Object v = dic.get("venue");

      if( v instanceof Map){
        Map v1 = (Map)dic.get("venue");
        venue =  new GeoPoint((Double)v1.get("lat"), (Double)v1.get("lon"));
      }

      if ( v instanceof List){
        List<Map> v1 = (List)dic.get("venue");
        List<GeoPoint> polygon =  new ArrayList<>();

        for( Map ve : v1) {
          GeoPoint p = new GeoPoint((Double)ve.get("lat"), (Double)ve.get("lon"));
          polygon.add(p);
        }
        venue = this.enclosePolygon(polygon);
      }

      else{

      }
    }

    if( dic.containsKey("hour")) {
      Map<String, Integer> h = (Map<String, Integer>) dic.get("hour");

      hour = new SimpleHour(h.get("hh"), h.get("mm"));
    }

    if( dic.containsKey("date")) {
      String dateInString = (String) dic.get("date");
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

      try {
        java.util.Date d = formatter.parse(dateInString);
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        date = new SimpleDate(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) +1 , cal.get(Calendar.YEAR));
      } catch (ParseException ex) {
        date = null;
      }
    }
  }

  /**
   * If the polygon is not closed.
   * Just correct the enclosure
   * @param polygon
   * @return
   */
  public List<GeoPoint> enclosePolygon(List<GeoPoint> polygon){
    if ( polygon.get(0).getLat() != polygon.get(polygon.size()-1).getLat())
      polygon.get(polygon.size()-1).resetLat(polygon.get(0).getLat());

    if ( polygon.get(0).getLon() != polygon.get(polygon.size() - 1).getLon())
      polygon.get(polygon.size() - 1).resetLon(polygon.get(0).getLon());

    return polygon;
  }

  /**
   * Print a representation object
   * @return
   */
  public String toString(){
    String res = "" + this.id + "\n";

    if ( this.venue != null)
      res += (this.venue + "\n");

    if ( this.date != null)
      res += (this.date + "\n");

    if ( this.hour != null)
      res += (this.hour + "\n");

    return res;
  }

}