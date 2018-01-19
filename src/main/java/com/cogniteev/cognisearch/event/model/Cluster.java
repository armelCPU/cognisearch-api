package com.cogniteev.cognisearch.event.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by armel on 07/07/17.
 */
public class Cluster {

  public Cluster(){
    elements = new ArrayList<>();
  }

  /**
   * Constructor from a map
   * @param dic
   */
  public Cluster(Map<String, Object> dic) {
    if ( dic.containsKey("name"))
      this.name = ((String) dic.get("name")).toLowerCase();

    if ( dic.containsKey("centroid"))
      this.centroid = dic.get("centroid");

    if ( dic.containsKey("elements"))
      this.elements = dic.get("elements") instanceof String ? Arrays.asList((String) dic.get("elements")) : (List < String >) dic.get("elements");
  }
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Object getCentroid() {
    return centroid;
  }

  public void setCentroid(Object centroid) {
    this.centroid = centroid;
  }

  public void setCentroidFromArray( List<String> tokens) {
    String s = "";
    for ( String token : tokens)
      s += (token + " ");
    this.centroid = s;
  }

  public List<String> getElements() {
    return elements;
  }

  public void setElements(List<String> elements) {
    this.elements = elements;
  }

  public String toString(){
    String res = "";
    res += "centroid : " + this.centroid + "\n";
    res += "elements : " + this.getElements() + "\n";
    return res;
  }

  private List<String> elements;
  private Object centroid;

}
