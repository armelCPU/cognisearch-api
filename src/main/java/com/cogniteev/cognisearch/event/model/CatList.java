package com.cogniteev.cognisearch.event.model;

import java.util.List;

/**
 * Created by armel on 13/01/18.
 */
public class CatList {
  public List<ResultCat> getResults() {
    return results;
  }

  public void setResults(List<ResultCat> results) {
    this.results = results;
  }

  private List<ResultCat> results;
}
