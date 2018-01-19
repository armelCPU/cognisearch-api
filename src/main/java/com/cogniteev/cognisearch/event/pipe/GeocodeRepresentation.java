package com.cogniteev.cognisearch.event.pipe;

/**
 * Created by armel on 07/04/17.
 */
public class GeocodeRepresentation {

  private Utils tools;

  public GeocodeRepresentation() {
    tools = new Utils("localhost", 9300);
  }
  public void findVenue(String id) {
    int start = 0;
    int size = 100;
  }
}
