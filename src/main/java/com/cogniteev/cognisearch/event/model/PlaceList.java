package com.cogniteev.cognisearch.event.model;

import java.util.List;

/**
 * Created by armel on 13/01/18.
 */
public class PlaceList {
  public List<ResultPlace> getResults() {
    return results;
  }

  public void setResults(List<ResultPlace> results) {
    this.results = results;
  }

  private List<ResultPlace> results;
}
