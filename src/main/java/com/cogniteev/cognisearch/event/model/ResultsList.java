package com.cogniteev.cognisearch.event.model;

import java.util.List;

/**
 * Created by armel on 05/01/18.
 */
public class ResultsList {
  public List<Result> getResults() {
    return results;
  }

  public void setResults(List<Result> results) {
    this.results = results;
  }

  private List<Result> results;
}
