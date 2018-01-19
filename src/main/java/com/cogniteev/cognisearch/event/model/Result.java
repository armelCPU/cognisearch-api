package com.cogniteev.cognisearch.event.model;

import java.util.List;

/**
 * Created by armel on 05/01/18.
 */
public class Result {
  private String name;

  public List<String> getCategories() {
    return categories;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getHour() {
    return hour;
  }

  public void setHour(String hour) {
    this.hour = hour;
  }

  public ResultVenue getVenue() {
    return venue;
  }

  public void setVenue(ResultVenue venue) {
    this.venue = venue;
  }

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public String getPrice() {
    return price;
  }

  public void setPrice(String price) {
    this.price = price;
  }

  public List<RessourcePojo> getPerformers() {
    return performers;
  }

  public void setPerformers(List<RessourcePojo> performers) {
    this.performers = performers;
  }

  public List<String> getSource() {
    return source;
  }

  public void setSource(List<String> source) {
    this.source = source;
  }

  private List<String> categories;
  private String date;
  private ResultVenue venue;
  private String hour;
  private String duration;
  private String price;
  private List<RessourcePojo> performers;
  List<String> source;

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  double score;
}
