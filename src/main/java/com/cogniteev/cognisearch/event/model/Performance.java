package com.cogniteev.cognisearch.event.model;

import org.elasticsearch.common.joda.time.LocalDate;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by armel on 07/12/16.
 */
public class Performance {
  private Venue venue = null;
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Performance.class);


  public SimpleDate getDate() {
    return date;
  }

  public void setDate(SimpleDate date) {
    this.date = date;
  }

  private SimpleDate date = null;

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public Venue getVenue() {
    return venue;
  }

  public void setVenue(Venue venue) {
    this.venue = venue;
  }

  public Price getPrice() {
    return price;
  }

  public void setPrice(Price price) {
    this.price = price;
  }

  public Hour getHour() {
    return hour;
  }

  public void setHour(Hour hour) {
    this.hour = hour;
  }

  private String duration = null;
  private Price price = null;
  private Hour hour = null;

  public List<Who> getOrganizers() {
    return organizers;
  }

  public void setOrganizers(List<Who> organizers) {
    this.organizers = organizers;
  }

  public List<String> getSource() {
    return source;
  }

  public void setSource(List<String> source) {
    this.source = source;
  }

  private List<Who> organizers = null;

  public List<Who> getPerformers() {
    return performers;
  }

  public void setPerformers(List<Who> performers) {
    this.performers = performers;
  }

  private List<Who> performers = null;
  private List<String> source = null;

  public String toString(){
    String res = "";
    res += "Venue : " + venue + "\n";
    res += "Date : " + date + "\n";
    res += "Duration : " + duration + "\n";
    res += "Price : " + price + "\n";
    res += "Hour : " + hour + '\n';
    res += "Performers : " + performers + "\n";
    res += "Organizers : " + organizers + "\n";
    return res;
  }

  /**
   * Equality of two performances
   * @param perf2
   * @return
   */
  public boolean equals(Object perf2) {
    if (!(perf2 instanceof Performance))
      return false;

    Performance p2 = (Performance)perf2;

    if (this.getDate() == p2.getDate() && this.getVenue() == p2.getVenue())
      if ( this.getHour() == null || p2.getHour() == null)
        return true;
      else
        if ( this.getHour() == p2.getHour())
          return  true;

    return false;
  }

  /**
   * Tells us if a performance is null
   * @return
   */
  public boolean nullPerformance()
  {
   return (venue == null && date == null && performers == null);
  }

  /**
   * Map a performance to JSON
   * @return
   */
  public Map<String, Object> toMap() {
    Map<String, Object> performance = new HashMap<>();

    if (venue != null)
      performance.put("venue", venue.toMap());

    if (date != null)
      performance.put("perfDate", new LocalDate(date.formatDate()));

    if (hour != null)
      performance.put("perfHour", hour.toMap());

    if ( price != null )
    performance.put("price", price.toMap());

    if ( duration != null)
      performance.put("duration", duration);

    if ( source != null && source.size() > 0)
      performance.put("source", source);

    if ( performers != null && performers.size() > 0)
      performance.put("performers", this.buildWhos(performers));

    if ( organizers != null && organizers.size() > 0)
      performance.put("organizers", this.buildWhos(organizers));

    return performance;
  }


  /**
   * Cast a list of Who Object to corresponding JSON
   * @param whos
   * @return
   */
  public List<Map<String, Object>> buildWhos(List<Who> whos) {
    List<Map<String, Object>> people = new ArrayList<>();

    for (Who who : whos) {
      people.add(who.toMap());
    }

    return people;
  }

  /**
   * Build a simple Date from util.Date
   * @param d
   * @return
   */
  public SimpleDate SimpleDateFromDate(java.util.Date d) {
    SimpleDate dat = new SimpleDate();

    try {
      Calendar cal = Calendar.getInstance();
      cal.setTime(d);
      dat.setDay(cal.get(Calendar.DAY_OF_MONTH));
      dat.setMonth(cal.get(Calendar.MONTH) + 1);
      dat.setYear(cal.get(Calendar.YEAR));
    }
    catch (Exception ex) {
      LOG.error("Error while parsing the date : " + ex.getMessage());
      return null;
    }

    return dat;
  }
}
