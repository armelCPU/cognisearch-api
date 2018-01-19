package com.cogniteev.cognisearch.event.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by armel on 07/12/16.
 */
public class SimpleHour {
  public int getHour() {
    return hour;
  }

  public void setHour(int hour) {
    this.hour = hour;
  }

  public int getMinutes() {
    return minutes;
  }

  public void setMinutes(int minutes) {
    this.minutes = minutes;
  }

  private int hour;
  private int minutes;

  public SimpleHour(){

  }

  public SimpleHour(int hour, int minutes){
    this.hour = hour;
    this.minutes = minutes;
  }

  public SimpleHour(Map dic){
    this.hour = (int) dic.get("hh");
    this.minutes = (int) dic.get("mm");
  }

  public String toString(){
    String hr = hour < 10 ? ("0" + hour) : ("" + hour);
    hr += "h";
    hr += minutes < 10 ? ("0"+minutes) : minutes;
    return hr;
  }

  public int getTimeInMinutes(){
    return hour*60 + minutes;
  }
  /**
   * Define the equality of two hour objects
   * @param hour2
   * @return
   */
  public boolean equals(Object hour2)
  {
    if (!(hour2 instanceof SimpleHour))
      return false;
    SimpleHour h2 = (SimpleHour) hour2;

    if (h2.getHour()==this.getHour() && h2.getMinutes()==this.getMinutes())
      return true;

    return false;
  }

  public Map<String, Object> toMap(){
    Map<String, Object> time = null;

    if (this != null)
    {
      time = new HashMap<>();

      time.put("hh", this.getHour());
      time.put("mm", this.getMinutes());

    }

    return time;
  }

}
