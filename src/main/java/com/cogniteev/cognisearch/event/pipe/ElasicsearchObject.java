package com.cogniteev.cognisearch.event.pipe;

import com.cogniteev.cognisearch.event.model.*;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.joda.time.LocalDate;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by armel on 26/12/16.
 */
public class ElasicsearchObject {
  private Client client;

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ElasicsearchObject.class);

  public  ElasicsearchObject(String host_name, int port_number)
  {
    client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(host_name, port_number));
  }

  /**
   * Convert a list of Clusters to a list of Maps
   * @param clusters
   * @return
   */
  List<Map<String, Object>> listOfClustersToMap(List<Cluster> clusters) throws ParseException {
    List<Map<String, Object>> res = new ArrayList<>();
    for (Cluster c : clusters) {
      Map<String, Object> o = new HashMap<>();
      o.put("name", c.getName());

      if ( c.getCentroid() instanceof SimpleDate){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        o.put("centroid", new LocalDate(formatter.parse(((SimpleDate)c.getCentroid()).toString())));
      }

      else{
        if ( c.getCentroid() instanceof SimpleHour)
          o.put("centroid", ((SimpleHour)c.getCentroid()).toMap());
        else
          o.put("centroid", c.getCentroid());
      }

      o.put("elements", c.getElements());

      res.add(o);
    }

    return  res;
  }

  /**
   * Index event clusters
   * @param title
   * @param cat
   * @param perf
   */
  public void indexEventClusters(List<Cluster> title, List<Cluster> cat, List<Cluster> perf) throws ParseException {
    Map<String, Object> json = new HashMap<>();
    json.put("title", this.listOfClustersToMap(title));
    json.put("category", this.listOfClustersToMap(cat));
    json.put("performer", this.listOfClustersToMap(perf));

     IndexResponse response = client.prepareIndex("clusters_events", "event_cluster")
            .setSource(json)
            .get();
    LOG.info("Object successfully indexed " + json);
  }

  /**
   * Index Clusters
   * @param venues
   * @param dates
   * @param hours
   */
  public void indexRepClusters( List<Cluster> venues, List<Cluster> dates, List<Cluster> hours) throws ParseException {
    Map<String, Object> json = new HashMap<>();
    json.put("venue", this.listOfClustersToMap(venues));
    json.put("date", this.listOfClustersToMap(dates));
    json.put("hour", this.listOfClustersToMap(hours));

    IndexResponse response = client.prepareIndex( "clusters_representations", "clusters_representation")
        .setSource(json)
        .get();
    LOG.info("Object successfully indexed " + json);
  }

  public boolean indexEvent(Event event) throws ParseException {

    try {
      Map<String, Object> json = new HashMap<String, Object>();

      if (event.getName() != null)
        json.put("name", event.getName());

      if (event.getCategories() != null)
        json.put("category", event.getCategories());



      if (event.getPerformances() != null) {
        List<Map<String, Object>> performances = new ArrayList<>();

        for (Performance perf : event.getPerformances()) {
          Map<String, Object> performance = new HashMap<>();

          if ( perf.getVenue() != null )
            performance.put("venue", perf.getVenue().toMap());

          if ( perf.getDate() != null)
            performance.put("perfDate", perf.getDate().toMap());

          if ( perf.getHour() != null )
            performance.put("perfHour", perf.getHour().toMap());

          if ( perf.getPrice() != null )
            performance.put("price", perf.getPrice().toMap());

          if ( perf.getDuration() != null)
            performance.put("duration", perf.getDuration());

          performances.add(performance);
        }

        if (performances.size() > 0)
          json.put("performances", performances);
        else
          json.put("performances", null);
      }

      try {

        /*IndexResponse response = client.prepareIndex("cognisearch_event_20161230", "event_type")
            .setSource(json)
            .get();*/
        LOG.info("Object successfully indexed " + json);
        return true;
      }
      catch (Exception ex){
        LOG.error("Error during object indexation " + ex.getMessage());
      }

      return true;
    }
    catch (Exception ex){
      LOG.error("Error in the object convertion in JSON");
      return false;
    }
  }
}
