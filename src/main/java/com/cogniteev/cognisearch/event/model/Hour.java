package com.cogniteev.cognisearch.event.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by armel on 07/12/16.
 */
public class Hour {
  public SimpleHour getStartHour() {
    return startHour;
  }

  public void setStartHour(SimpleHour startHour) {
    this.startHour = startHour;
  }

  public SimpleHour getEndHour() {
    return endHour;
  }

  public void setEndHour(SimpleHour endHour) {
    this.endHour = endHour;
  }

  private SimpleHour startHour;
  private SimpleHour endHour;

  public String toString(){
    if(startHour != null && endHour != null)
      return "entre " + startHour + "et" + endHour + "\n";
    else
      return startHour + " ";
  }


  /**
   * Equality between two hours
   * @param hour2
   * @return
   */
  public boolean equals(Object hour2)
  {
    if (!(hour2 instanceof Hour))
      return false;

    Hour h2 = (Hour)hour2;

    if(this.getStartHour() == h2.getStartHour() && this.getEndHour() == h2.getEndHour())
      return true;

    return false;
  }

  /**
   * This functions parse an Hour object to Json
   * @return
   */
  public Map<String, Object> toMap(){
    Map<String, Object> hour = new HashMap<>();

    if (this.getStartHour() != null)
    {
      Map<String, Object> starthour = new HashMap<>();
      starthour.put("hh", this.getStartHour().getHour());
      starthour.put("mm", this.getStartHour().getMinutes());

      hour.put("startHour", starthour);
    }

    if ( this.getEndHour() != null )
    {
      Map<String, Object> endHour = new HashMap<>();
      endHour.put("hh", this.getEndHour().getHour());
      endHour.put("mm", this.getEndHour().getHour());

      hour.put("endHour", endHour);
    }

    return  hour;
  }
}
