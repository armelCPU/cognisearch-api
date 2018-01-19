package com.cogniteev.cognisearch.event.model;

import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by armel on 07/12/16.
 */
public class SimpleDate {
  private String jourSem = "";
  private int day;

  public int getMonth() {
    return month;
  }

  public void setMonth(int month) {
    this.month = month;
  }

  public int getDay() {
    return day;
  }

  public void setDay(int day) {
    this.day = day;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  private int month;
  private int year;

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SimpleDate.class);

  public String toString(){
    return jourSem + " "  + day +"/" + month +"/" + year + "\n";
  }

  public  SimpleDate(){

  }


  public SimpleDate(int day, int month, int year) {
    this.day = day;
    this.month = month;
    this.year = year;
  }

  /**
   * Construct a simple date from the string returned by ES
   * @param dateString
   */
  public SimpleDate(String dateString) throws ParseException {

    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    java.util.Date datetime = formatter.parse(dateString);

    try {
      Calendar cal = Calendar.getInstance();
      cal.setTime(datetime);
      this.setDay(cal.get(Calendar.DAY_OF_MONTH));
      this.setMonth(cal.get(Calendar.MONTH) + 1);
      this.setYear(cal.get(Calendar.YEAR));
    }
    catch (Exception ex) {
      LOG.error("Error while parsing the date : " + ex.getMessage());
    }
  }
  /**
   * This function calculates the equality between two simple dates
   * @param date2
   * @return
   */
  public boolean equals(Object date2)
  {
    if (! (date2 instanceof SimpleDate))
      return false;

    SimpleDate d2 = (SimpleDate)date2;

    if(this.getDay() == d2.getDay() && this.getMonth() == d2.getMonth() && this.getYear() == d2.getYear())
      return true;

    return false;

  }


  /**
   * This function parse a date to a json Object
   * @return
   * @throws java.text.ParseException
   */
  public Map<String, Object> toMap() {
    Map<String, Object> date = new HashMap<>();

    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    String dateInString = "";

      dateInString = this.toString();
      try {
        date.put("perfDate", formatter.parse(dateInString));
      } catch (ParseException ex) {
        date = null;
        LOG.error("Error during the start date parsing");
      }
    return  date;
  }

  /**
   * Format Simple date for indexation
   * @return
   */
  public java.util.Date formatDate() {
    Date date = null;
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    String dateInString = "";

    dateInString = this.toString();
    try {
       date = formatter.parse(dateInString);
    } catch (ParseException ex) {
      date = null;
      LOG.error("Error during the start date parsing");
    }
    return date;
  }

  /**
   * This function computes the number of days of a date from January 1st, 1970.
   * Retrune -1, if there is an error
   * @return
   * @throws ParseException
   */
  public long dateInDays() {
    long interval = 24*1000 * 60 * 60;
    try{
      DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
      java.util.Date datetime = formatter.parse(this.toString());
      return datetime.getTime()/interval;
    }
    catch (Exception ex){
      LOG.error("Error while parsing the date : " + ex.getMessage());
      return  -1;
    }
  }

}
