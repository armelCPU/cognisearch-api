package com.cogniteev.cognisearch.event.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by armel on 24/01/17.
 */
public class Who {
  private  String name;

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String uri = null;

  /**
   * Print a Person or an Organisation
   * @return
   */
  public String toString() {
    String r = "name : " + name + "\n";
    if (uri != null)
      r += "uri : " + uri + "\n";

    return r;
  }

  /**
   * Convert Object to Map
   * @return
   */
  public Map<String, Object> toMap() {
    Map<String, Object> obj = new HashMap<String, Object>();

    if (name != null)
      obj.put("name", name);

    if (uri != null)
      obj.put("uri", uri);

    return obj;

  }
}
