package com.cogniteev.cognisearch.event.model;

import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by armel on 07/12/16.
 */
public class Date {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Date.class);

  public SimpleDate getStartDate() {
    return startDate;
  }

  public void setStartDate(SimpleDate startDate) {
    this.startDate = startDate;
  }

  public SimpleDate getEndDate() {
    return endDate;
  }

  public void setEndDate(SimpleDate endDate) {
    this.endDate = endDate;
  }

  private SimpleDate startDate = null;
  private SimpleDate endDate = null;


  public String toString(){
    String d = "";
    if(endDate != null)
      d = "entre le " + startDate + "et le " + endDate + "\n";
    else
      d = startDate.toString();
    return d;
  }

  /**
   * Equality between two dates
   * @param date2
   * @return
   */
  public boolean equals(Object date2) {
    if ( !(date2 instanceof Date))
      return false;

    Date d2 = (Date)date2;

    if (this.getStartDate() == d2.getStartDate() && this.getEndDate() == d2.getEndDate())
      return true;

    return false;
  }

  /**
   * This function parse a date to a json Object
   * @return
   * @throws ParseException
   */
  public Map<String, Object> toMap() throws ParseException {
    Map<String, Object> date = new HashMap<>();

    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    String dateInString = "";

    if(this.getStartDate() != null)
    {
      dateInString = this.getStartDate().toString();
      try{
        date.put("startDate", formatter.parse(dateInString));
      }
      catch (Exception ex){
        date.put("startDate", null);
        LOG.error("Error during the start date parsing");
      }
    }

    if(this.getEndDate() != null)
    {
      dateInString = this.getEndDate().toString();
      try{
        date.put("endDate", formatter.parse(dateInString));
      }
      catch (Exception ex){
        date.put("endDate", null);
        LOG.error("Error during the end date parsing");
      }
    }

    return date;
  }

  /**
   * Tells if a date is a interval date or not
   * @return
   */
  public boolean isIntervalDate() {
    return ( startDate!= null && endDate != null && !startDate.equals(endDate));
  }

  /**
   * Get all the dates between two dates
   * See : http://stackoverflow.com/questions/2689379/how-to-get-a-list-of-dates-between-two-dates-in-java
   * @return
   * @throws ParseException
   */
  public List<java.util.Date> getAllDates() throws ParseException {

    DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

    if (startDate == null || endDate == null)
      return null;

    try {
      List<java.util.Date> dates = new ArrayList<>();

      java.util.Date beginDate = formatter.parse(startDate.toString());
      java.util.Date finishDate = formatter.parse(endDate.toString());

      long cptTime = beginDate.getTime();
      long finishTime = finishDate.getTime();
      long interval = 24*1000 * 60 * 60; // 1 day in millis


      while ( cptTime <= finishTime)
      {
        dates.add(new java.util.Date(cptTime));
        cptTime += interval;
      }

      return dates;
    }
    catch (Exception ex) {
      LOG.info("Error while analysing the dates here : " + ex.getMessage());
      return null;
    }

  }


}
