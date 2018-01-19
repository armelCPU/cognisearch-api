package com.cognisearch.helpers;

import com.cogniteev.cognisearch.event.model.*;
import com.cogniteev.cognisearch.event.similarity.Utils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.elasticsearch.index.query.FilterBuilders.geoDistanceFilter;
import static org.elasticsearch.index.query.FilterBuilders.geoPolygonFilter;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

/**
 * Created by armel on 04/01/18.
 */
public class EsWorker {
  private static int AREA_RADUIS = 3;

  private double EVENT_WEIGHT = 0.35;

  private double REP_WEIGHT   = 0.65;

  private int RETRIEVED = 15;

  private int K = 10;

  private Client client;

  private String index;

  private String type;

  private SimilarityWorker similarityWorker;

  private Utils tools;

  private String ontologyFile;


  public EsWorker(String host_name, int port_number, String index, String type, String ontologyFile){
    client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(host_name, port_number));
    this.index = index;
    this.type = type;
    similarityWorker = new SimilarityWorker();
    tools = new Utils();
    this.ontologyFile = ontologyFile;
  }

  /**
   * Building the representation object of a query
   * @param params
   * @return
   */
  public RepEntity buildRepOfQuery(Map<String, Object> params){
    RepEntity repQuery = new RepEntity();

    if (params.containsKey("venue")) {
      List<GeoPoint> points = (List<GeoPoint>) params.get("venue");

      if (points.size() < 2)
        repQuery.setVenue(points.get(0));
      else
        repQuery.setVenue(points);
    }

    if (params.containsKey("date")) {
      String dateString = (String) params.get("date");

      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDate date;
      try {
        java.util.Date d = formatter.parse(dateString);
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        date = new SimpleDate(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) +1 , cal.get(Calendar.YEAR));
      } catch (ParseException ex) {
        date = null;
      }
      repQuery.setDate(date);
    }
    return repQuery;
  }

  /**
   * Building of the event part of a query
   * @param params
   * @return
   */
  public EventEntity buildEventOfQuery(Map<String, Object> params){
    EventEntity e = new EventEntity();

    if ( params.containsKey("name")) {
      String name = (String) params.get("name");
      e.setTitle(name);
    }

    if ( params.containsKey("category")) {
      List<String> cats = (List<String>) params.get("category");
      e.setCategories(cats);
    }

    if ( params.containsKey("performers")) {
      List<String> perfs = (List<String>) params.get("performers");
      e.setPerformers(perfs);
    }
    return e;
  }

  /**
   * This function helps to find events with a set of parameters
   * @param params
   * @return
   */
  public ResultsList simpleSearch(Map<String, Object> params) throws IOException, SAXException, ParserConfigurationException {
    List<Result> res = new ArrayList<>();

    BoolQueryBuilder qb = QueryBuilders.boolQuery();

    /**
     * Building the category part of the query
     */
    if (params.containsKey("category")){
      List<String> cats = (List<String>) params.get("category");

      // Add all categories to the match query
      for(String cat : cats) {
        qb.should(QueryBuilders.termQuery("category.uri", cat));
      }
    }

    /**
     * Building the performer part of the query
     */
    if (params.containsKey("performers")){
      List<String> perfs = (List<String>) params.get("performers");

      BoolQueryBuilder nestQuery = QueryBuilders.boolQuery();

      // Add all categories to the match query
      for(String perf : perfs) {
        nestQuery.should(QueryBuilders.matchQuery("performances.performers.name", perf));
      }

      NestedQueryBuilder perfQuery = nestedQuery("performances", nestQuery);
      qb.should(perfQuery);
    }
    /**
     * Building spatial part of the query
     */
    if (params.containsKey("venue")){
      List<GeoPoint> points = (List<GeoPoint>) params.get("venue");

      if( points.size() < 2){
        // This is a single point
        GeoDistanceFilterBuilder pointFilter = geoDistanceFilter("performances.venue.point")
            .point(points.get(0).getLat(), points.get(0).getLon())
            .distance(this.AREA_RADUIS, DistanceUnit.KILOMETERS)
            .optimizeBbox("memory")
            .geoDistance(GeoDistance.ARC);

        NestedQueryBuilder venueQuery = nestedQuery("performances", pointFilter);
        qb.should(venueQuery);
      }
      else{
        // This is a polygon
        GeoPolygonFilterBuilder polygonFilt = geoPolygonFilter("performances.venue.point");
        for(GeoPoint p : points){
          polygonFilt.addPoint(p);
        }
        NestedQueryBuilder venueQuery = nestedQuery("performances", polygonFilt);
        qb.should(venueQuery);
      }
    }

    /**
     * Building query for the date yyyy-MM-dd
     */
    if (params.containsKey("date")){
      String date = (String) params.get("date");

      QueryBuilder datefilter = QueryBuilders.termQuery("performances.perfDate", date);

      NestedQueryBuilder dateQuery = nestedQuery("performances", datefilter);
      qb.should(dateQuery);
    }
    SearchResponse resp = client.prepareSearch(this.index).setTypes(this.type).setSize(this.RETRIEVED).setQuery(qb).execute().actionGet();
    //this.display(resp.getHits().getHits());

    SearchHit[] hits = resp.getHits().getHits();
    RepEntity queryRep = this.buildRepOfQuery(params);
    EventEntity queryEvent = this.buildEventOfQuery(params);
    res = this.findRelevantRep(hits, queryRep, queryEvent);
    // for each event here we have to find the most relevant performance
    ResultsList r = new ResultsList();
    r.setResults(res);
    return r;
  }

  /**
   * This function returns the representation of each event with is the most relevant for the query
   * @param hits
   * @param queryRep
   * @return List<Map<String, Object>>
   */
  public List<Result> findRelevantRep(SearchHit[] hits, RepEntity queryRep, EventEntity queryEvent) throws ParserConfigurationException, SAXException, IOException {
    List<Result> results = new ArrayList<>();
    for (SearchHit hit : hits){
      double max = -10.0;
      double score;
      List<RepEntity> reps = this.getPerformances(hit.getSource());
      int pos  = -1;
      int i = 0;
      for(RepEntity r : reps) {
        score = this.similarityWorker.representationSimilarity(r, queryRep, 5.0);
        if ( score > max) {
          pos = i;
          max = score;
        }
        i++;
      }
      if ( pos >= 0){
        Result item = this.buildReturnEvent(hit.getSource(), pos);
        double globalScore = this.computeEventGlobalScore(this.similarityWorker.eventSimilarity(this.getEventPart(hit.getSource()), queryEvent, 5.0),
            this.similarityWorker.representationSimilarity(reps.get(pos), queryRep, 5.0));

        item.setScore(globalScore);
        results.add(item);
      }
    }

    results = this.sortResults(results);
    return  this.topK(results, this.K);
  }

  /**
   * Sort a list of event responses by relevance score
   * @param results
   * @return
   */
  private List<Result> sortResults(List<Result> results){

    for ( int i=0; i < results.size(); i++) {
      for ( int j=i+1; j< results.size(); j++) {
        if ( results.get(j).getScore() > (double)results.get(i).getScore()){
          // Permutation
          Result temp = results.get(j);
          results.set(j, results.get(i));
          results.set(i, temp);
        }
      }
    }

    return results;
  }

  /**
   * This functions sort the result list and takes the topK events
   * @param results
   * @param k
   * @return
   */
  private List<Result> topK(List<Result> results, int k){
    List<Result> res = new ArrayList<>();

    for( int i=0; i<results.size() && i <k ; i++){
      if ( results.get(i).getName() == null || results.get(i).getName().equalsIgnoreCase(""))
        continue;

      res.add(results.get(i));
    }
    return  res;
  }

  /**
   * Compute event score
   * @param event_score
   * @param rep_score
   * @return
   */
  private double computeEventGlobalScore(double event_score, double rep_score) {
    return this.EVENT_WEIGHT*event_score + this.REP_WEIGHT*rep_score;
  }
  /**
   * This function processes an event hit and the position of the best performance corresponding to a query to.
   * It's a json Object ready to be process in the frontend module
   * @param hit
   * @param rep_pos
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  Result buildReturnEvent(Map hit, int rep_pos) throws IOException, SAXException, ParserConfigurationException {

    //Map<String, Object> item = new HashMap<>();

    Result res = new Result();

    // Name
    if (hit.containsKey("name") && hit.get("name") != null){
      //item.put("name", hit.get("name"));
      res.setName((String) hit.get("name"));
    }


    // Categories
    if(hit.containsKey("category") && hit.get("category") != null){
      List<Map<String, Object>> categories = (List<Map<String, Object>>) hit.get("category");
      List<String> cats = new ArrayList<>();
      for(Map c : categories){
        Map<String, Object> catNode = this.tools.getNodeConcept(this.ontologyFile, (String)c.get("uri"));
        if (catNode != null)
          cats.add((String) catNode.get("name"));
      }
      //item.put("categories", cats);
      res.setCategories(cats);
    }


    // Processing performance
    List<Map<String, Object>> perfs = (List<Map<String, Object>>) hit.get("performances");
    Map<String, Object> resp = perfs.get(rep_pos);

    // date
    if ( resp.containsKey("perfDate") && resp.get("perfDate") != null) {

      String dateInString = (String) resp.get("perfDate");
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

      try {
        java.util.Date d = formatter.parse(dateInString);
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        SimpleDate date = new SimpleDate(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) +1 , cal.get(Calendar.YEAR));
        //item.put("date", date);
        res.setDate(date.toString());
      } catch (ParseException ex) {
        //item.put("date", null);
      }

    }

    // venue
    if ( resp.containsKey("venue") && resp.get("venue") != null) {
      //item.put("venue", resp.get("venue"));
      ResultVenue eventVenue = new ResultVenue();
      Map<String, Object> place = (Map<String, Object>)resp.get("venue");

      // il we were able to geocode the adresse
      if (place.containsKey("point")) {
        Map<String, Object> point = (Map<String, Object>)place.get("point");

        eventVenue.setCity((String)place.get("councilName"));

        String addSupp = place.containsKey("addSupplement") ? (String)place.get("addSupplement") : "";
        String addString = place.containsKey("addressString") ? (String)place.get("addressString") : "";
        addString = addString.replace("null", "").trim();
        eventVenue.setName(addString);

        Resultpoint pt = new Resultpoint();
        pt.setLat((Double) point.get("lat"));
        pt.setLon((Double) point.get("lon"));
        eventVenue.setLocation(pt);

        res.setVenue(eventVenue);
      }

    }

    //hour
    if ( resp.containsKey("perfHour") && resp.get("perfHour") != null) {
      Map hour = (Map) resp.get("perfHour");
      Map startHour = (Map) hour.get("startHour");

      SimpleHour hr = new SimpleHour((int)startHour.get("hh"), (int)startHour.get("mm"));
      //item.put("hour", hr.toString());
      res.setHour(hr.toString());
    }

    // Duration
    if ( resp.containsKey("duration") && resp.get("duration") != null){
      //item.put("duration", resp.get("duration"));
      res.setDuration((String)resp.get("duration"));
    }

    // Price
    if ( resp.containsKey("price") && resp.get("price") != null) {
      Map<String, Object> priceObj = (Map<String, Object>)resp.get("price");
      String value = "";
      double min = (double)priceObj.get("minPrice");
      String cur = (String)priceObj.get("currency");

      value += "" + min;

      if (priceObj.containsKey("maxPrice")){
        double max = (double)priceObj.get("maxPrice");
        value += (" - " + max);
      }

      value += " " + cur;
      //item.put("price", value);
      res.setPrice(value);
    }

    // Source
    if( resp.containsKey("source") && resp.get("source") != null){
      //item.put("source", resp.get("source"));
      res.setSource((List<String>) resp.get("source"));
    }

    // Performers
    if( resp.containsKey("performers") && resp.get("performers") != null) {
      List<Map<String, Object>> performers = (List<Map<String, Object>>) resp.get("performers");
      List<RessourcePojo> pfs = new ArrayList<>();

      for(Map p : performers){
        RessourcePojo act = new RessourcePojo();
        act.setName((String) p.get("name"));
        act.setUri((String) p.get("uri"));

        pfs.add(act);
      }
      //item.put("performers", resp.get("performers"));

      res.setPerformers(pfs);
    }

    return res;
  }

  /**
   * This function extracts the event part of an event entity
   * @param event
   * @return
   */
  public EventEntity getEventPart(Map event){
    EventEntity e = new EventEntity();

    if (event.containsKey("name"))
      e.setTitle((String) event.get("name"));

    if (event.containsKey("category")){
      List<String> cats = new ArrayList<>();
      List<Map<String, Object>> categories = (List<Map<String, Object>>) event.get("category");

      for(Map c : categories){
        cats.add((String)c.get("uri"));
      }

      e.setCategories(cats);
    }

    if (event.containsKey("performances")){
      List<Map<String, Object>> perfs = (List<Map<String, Object>>) event.get("performances");
      List<String> mainPerf = new ArrayList<>();

      int i = 0;
      for (Map p : perfs) {
        if (p.containsKey("performers")){
          List<Map> pS = (List<Map>) p.get("performers");

          for (Map pi : pS){
            // Find if this performer is in the order performances
            boolean isMainPerformer = true;
            String piName = (String)pi.get("name");
            for(int j=0; j<perfs.size(); j++){
              if ( i!=j && perfs.get(j).containsKey("performers")){
                List<Map> tempPerfs = (List<Map>) perfs.get(j).get("performers");
                List<String> tempPerfsName = new ArrayList<>();

                for( Map tempP : tempPerfs){
                  tempPerfsName.add((String)tempP.get("name"));
                }

                if ( !tempPerfsName.contains(piName)) {
                  isMainPerformer = false;
                  break;
                }
              }
            }

            if ( isMainPerformer)
              mainPerf.add(piName);
          }
        }
        i++;
      }

      // Remove duplicate performers names
      Set<String> deduplicated = new HashSet<>();
      deduplicated.addAll(mainPerf);
      mainPerf.clear();
      mainPerf.addAll(deduplicated);

      // Adding all Perf to entity
      e.setPerformers(mainPerf);
    }
    return e;
  }

  /**
   * Get all the performances related to a event
   * @param event
   * @return
   */
  public List<RepEntity> getPerformances(Map event){
    List<RepEntity> res = null;
    if (event.containsKey("performances")){
      List<Map<String, Object>> perfs = (List<Map<String, Object>>) event.get("performances");
      res = new ArrayList<>();

      for (Map p :perfs) {
        RepEntity rep = new RepEntity();

        // Edit the venue property
        if (p.containsKey("venue")) {
          Map v = (Map) p.get("venue");

          if( v.containsKey("polygon")){
            List<Map> points = (List<Map>) v.get("polygon");
            List<GeoPoint> pts = new ArrayList<>();

            for(Map pt : points){
              GeoPoint point = new GeoPoint((double)pt.get("lat"), (double)pt.get("lon"));
              pts.add(point);
            }

            rep.setVenue(pts);
          }
          else {
            if ( v.containsKey("point") && v.get("point") == null){
              Map pt = (Map) v.get("point");
              GeoPoint point = new GeoPoint((double)pt.get("lat"), (double)pt.get("lon"));
              rep.setVenue(point);
            }
            else
              rep.setVenue(null);
          }
        }

        // Edit the date Prop
        if (p.containsKey("perfDate")) {
          String dateInString = (String) p.get("perfDate");
          SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

          try {
            java.util.Date d = formatter.parse(dateInString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            SimpleDate date = new SimpleDate(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) +1 , cal.get(Calendar.YEAR));
            rep.setDate(date);
          } catch (ParseException ex) {
            rep.setDate(null);
          }
        }

        // Edit Hour Prop
        if (p.containsKey("perfHour")){
          Map hour = (Map) p.get("perfHour");
          Map startHour = (Map) hour.get("startHour");

          SimpleHour hr = new SimpleHour((int)startHour.get("hh"), (int)startHour.get("mm"));

          rep.setHour(hr);
        }
        res.add(rep);
      }
    }

    return res;
  }

  /**
   * This function gets all existing categories in an OWL file
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public CatList getAllCategories() throws IOException, SAXException, ParserConfigurationException {
    CatList r= new CatList();
    r.setResults(new ArrayList<ResultCat>());

    List<Map<String, Object>> cats = tools.getNodes(this.ontologyFile);

    int i = 1;
    for(Map cat : cats) {
      ResultCat c = new ResultCat();
      c.setId(i);
      c.setName((String) cat.get("name"));
      c.setUri((String) cat.get("uri"));

      r.getResults().add(c);
      i++;
    }

    return r;
  }

  /**
   * Getting all the places indexed
   * @return
   */
  public PlaceList getAllPlaces(){
    PlaceList place1 = this.tools.getAllPlaces("cognisearch_pois", "poi_type", 1);
    PlaceList place2 = this.tools.getAllPlaces("cognisearch_cities", "city_type", 2);

    for ( ResultPlace r : place1.getResults()){
      place2.getResults().add(r);
    }

    return  place2;
  }

  public CatList getAllPerformers(){
    CatList res = this.tools.getAllPerformers("cognisearch_performers", "performer_type");
    return  res;
  }

  private void display(SearchHit[] hits){
    for(SearchHit hit : hits)
    {
      /*try{
        List<Map> perfs = (List<Map>)hit.getSource().get("performances");
        System.out.println(perfs.get(0).get("perfDate"));
      }
      catch (Exception ex){

      }*/
      // System.out.println(hit.getScore());
      //System.out.println(this.getEventPart(hit.getSource()));
    }
  }

  public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
    EsWorker worker = new EsWorker("localhost", 9300, "cognisearch_event_20171231", "event_type", "resources/categoriesEvt.xml");

    Map<String, Object> params = new HashMap<>();
    params.put("category", Arrays.asList("http://www.owl-ontologies.com/categoriesEvt.owl#Humour"));
    //params.put("performers", Arrays.asList("Jean Marie Bigard"));
    //params.put("venue", Arrays.asList(new GeoPoint(48.80, 2.00), new GeoPoint(49.98, 4.24), new GeoPoint(50.02, 1.02), new GeoPoint(48.80, 2.00)));
    //params.put("venue", Arrays.asList(new GeoPoint(48.80, 2.00)));
    //params.put("date", "2018-05-31");
    //worker.simpleSearch(params);
    //System.out.println(worker.buildRepOfQuery(params));
    System.out.println(worker.getAllPerformers());
    /*Client client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
    SearchResponse resp = client.prepareSearch("cognisearch_pois").setTypes("poi_type").setFrom(0).setSize(100).execute().actionGet();
    SearchHit[] hits = resp.getHits().getHits();
    System.out.println(hits.length);*/
  }

}
