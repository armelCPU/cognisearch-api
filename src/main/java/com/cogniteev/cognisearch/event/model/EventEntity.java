package com.cogniteev.cognisearch.event.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by armel on 25/05/17.
 */
public class EventEntity {
  private String title = null;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<String> getCategories() {
    return categories;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
  }

  public List<String> getPerformers() {
    return performers;
  }

  public void setPerformers(List<String> performers) {
    this.performers = performers;
  }

  private List<String> categories = null;
  private List<String> performers = null;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  private String id;

  public EventEntity(){

  }

  public EventEntity(String title, List<String> categories, List<String> performers){
    this.title = title;
    this.categories = categories;
    this.performers = performers;
  }

  /**
   * Construction of an EventEntity object for similarity calculation from the source output of Elasticsearch
   * @param dic
   */
  public EventEntity(Map<String, Object> dic, String id) {
    if ( dic.containsKey("name"))
      this.title = ((String) dic.get("name")).toLowerCase();

    if ( dic.containsKey("performers")){
      this.performers = new ArrayList<>();

      if (dic.get("performers") instanceof List){
        List<Map<String, Object>> perfs = (List<Map<String, Object>>) dic.get("performers");
        for ( Map perf : perfs)
          this.performers.add(((String) perf.get("name")).toLowerCase());
      }
      else
        this.performers.add(((String) ((Map) dic.get("performers")).get("name")).toLowerCase());
    }


    if ( dic.containsKey("category")) {
      this.categories = new ArrayList<>();

      if (dic.get("category") instanceof List ){
        List<Map<String, Object>> cats = (List<Map<String, Object>>) dic.get("category");
        for ( Map cat : cats)
          this.categories.add((String) cat.get("uri"));
      }
      else
        this.categories.add((String) ((Map) dic.get("category")).get("uri"));
    }

    this.id = id;
  }

  /**
   * Print an EventEntity contain
   * @return
   */
  public String toString() {
    String res = "";

    if (this.title != null)
      res += (this.title + "\n");

    if (this.performers != null)
      res += (this.performers + "\n");

    if ( this.categories != null)
      res += (this.categories + "\n");

    return res;
  }
}
