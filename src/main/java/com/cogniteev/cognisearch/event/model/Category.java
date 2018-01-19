package com.cogniteev.cognisearch.event.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by armel on 17/03/17.
 */
public class Category {
  private String uri;

  public List<String> getLabels() {
    return labels;
  }

  public void setLabels(List<String> labels) {
    this.labels = labels;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public int getOccurrences() {
    return occurrences;
  }

  public void setOccurrences(int occurrences) {
    this.occurrences = occurrences;
  }

  private List<String> labels;
  private int occurrences;

  /**
   * Print a Person or an Organisation
   * @return
   */
  public String toString() {
    String r = "labels : " + labels + "\n";
    r += "occurrences : " + occurrences + "\n";
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

    if (labels != null)
      obj.put("labels", labels);

    if (uri != null)
      obj.put("uri", uri);

    if (occurrences != 0)
      obj.put("occurrences", occurrences);

    return obj;

  }
}
