package com.cogniteev.cognisearch.event.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by armel on 07/12/16.
 */
public class Event {
  private String name = null;

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<Category> getCategories() {
    return categories;
  }

  public void setCategories(List<Category> categories) {
    this.categories = categories;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String description = null;
  private List<Category> categories = null;

  public List<Performance> getPerformances() {
    return performances;
  }

  public void setPerformances(List<Performance> performances) {
    this.performances = performances;
  }

  private List<Performance> performances = null;

  public String toString(){
    String res = "\n";
    res += name + "\n";
    res += "Categories : " + categories + "\n";
    res += "Performances :" + performances + "\n";
    return res;
  }

  public boolean performanceInEvent(Performance p){
    for(Performance perf : this.getPerformances()){
      if(perf == p)
        return true;
    }
    return false;
  }

  /**
   * Return whether a performance is in a list or not
   * @param perfs
   * @param p
   * @return
   */
  public boolean performanceInPerformances(List<Performance> perfs, Performance p){
    for(Performance perf : perfs){
      if(perf == p)
        return true;
    }
    return false;
  }


  /**
   * This function checks if a String is in a List
   * @param l
   * @param s
   * @return
   */
  public boolean listContainsValue(List<String> l, String s)
  {
    for (String str : l ) {
      if (str.equalsIgnoreCase(s))
        return true;
    }
    return false;
  }

  /**
   * This function removes duplicate elements from a List of String
   * @param l
   * @return
   */
  public List<String> deduplicate(List<String> l) {
    List<String> res = new ArrayList<>();

    for (String s : l) {
      // Skip ",", "."
      if (s.equalsIgnoreCase(".") || s.equalsIgnoreCase(",") || s.equalsIgnoreCase("Riez !"))
        continue;

      if (!listContainsValue(res, s)){
        res.add(s);
      }
    }

    return res;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> event = new HashMap<String, Object>();

    if (name != null)
      event.put("name", name);

    if (categories != null && categories.size() > 0)
      event.put("category", this.buildCategoriesJSON(categories));

    if (description != null)
      event.put("description", description);


    if (performances != null && performances.size() > 0)
      event.put("performances", this.buildPerformanceJSON(performances));
    return  event;
  }


  /**
   * Cast a list of Performances to JSON object ready to index
   * @param perfs
   * @return
   */
  public List<Map<String, Object>> buildPerformanceJSON(List<Performance> perfs) {
    List<Map<String, Object>> performs = new ArrayList<>();

    for (Performance perf : perfs) {
      performs.add(perf.toMap());
    }

    return performs;
  }


  /**
   * Cast a list of Categories to object ready to index
   * @param cats
   * @return
   */
  public List<Map<String, Object>> buildCategoriesJSON(List<Category> cats){
    List<Map<String, Object>> categories = new ArrayList<>();

    for (Category cat : cats)
      categories.add(cat.toMap());

    return categories;
  }
}
