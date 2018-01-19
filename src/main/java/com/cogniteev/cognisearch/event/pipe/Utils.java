package com.cogniteev.cognisearch.event.pipe;

import com.cogniteev.cognisearch.event.model.Event;
import com.cogniteev.cognisearch.event.model.Place;
import com.cogniteev.cognisearch.event.model.Who;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Created by armel on 26/01/17.
 */
public class Utils {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Utils.class);

  private static final Double THRESHOLD  = 0.85;

  private String api_key = "AIzaSyD_B84MC2GON5qYTOcRMTaTibfyr0Ce--U";
  // private String api_key = "AIzaSyBtX4YUEVdWBW069iuPOgVtpjeFbFpHFQk";

  Client client;

  String es_host;

  int es_port;

  List<String> backlog = new ArrayList<>();

  public Utils(String host, int port) {
    this.es_host = host;
    this.es_port = port;
    client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(host, port));
  }
  /**
   * Build a list of Who from a List of String
   * @param people
   * @return
   */
  public List<Who> buildWhoFromString (List<String> people, String host, int port ) {

    if (people == null || people.size() < 1)
      return  null;

    List<Who> whos = new ArrayList<>();

    for (String s : people) {
      Who p = new Who();
      p.setName(s);

      // Query ES to get the URL
      Map<String, Object> whoOjbect = this.searchByField("cognisearch_performers", "performer_type", "labels", s);

      if (whoOjbect != null && whoOjbect.containsKey("uri")) {
        p.setUri((String) whoOjbect.get("uri"));
        List<String> labels = (List<String>) whoOjbect.get("labels");
        p.setName(labels.get(0));
      }

      whos.add(p);
    }

    return whos;
  }

  /**
   * This function geocodes hit representations which do not have any points information
   * @param hit
   * @return
   */
  public List<Map<String, Object>> checkAndGeocode(Map<String, Object> hit) {
    List<Map<String, Object>> reps = (List<Map<String, Object>>) hit.get("performances");
    Map<String, Object> venue = null;

    List<Map<String, Object>> new_reps = new ArrayList<>();

    for( Map<String, Object> rep : reps) {
      venue = (Map<String, Object>) rep.get("venue");
      if ( venue != null && !venue.containsKey("polygon") && !venue.containsKey("point")) {

        if( venue.containsKey("addSupplement")) {
          String name = (String)venue.get("addSupplement");
          // find in the gazetier
          GeoPoint pt = null;
          Map p = this.searchByField("cognisearch_pois", "poi_type", "labels", name);


          if (p != null) {
            Map<String, Double> point = (Map<String, Double>) p.get("point");
            pt = new GeoPoint(point.get("lat"), point.get("lon"));
            ((Map<String, Object>) rep.get("venue")).put("point", pt);
          }
          else {
            //Querying google
            if (! backlog.contains(name)) {
              backlog.add(name);
              Place place = this.queryGoogle(name);
              if (place != null) {
                pt = new GeoPoint(place.getLat(), place.getLon());
                ((Map<String, Object>) rep.get("venue")).put("point", pt);
                this.index_place(place, "localhost", 9300);

              }
            }
            else
              LOG.info("Not Necessary Query !");
          }

        }
        else
        {
          // Query Google with the city
          if( venue.containsKey("councilName")) {

            if(! backlog.contains(venue.get("councilName"))) {
              //Querying google
              backlog.add((String) venue.get("councilName"));
              GeoPoint pt = null;
              Place place = this.queryGoogle((String) venue.get("councilName"));
              if (place != null) {
                pt = new GeoPoint(place.getLat(), place.getLon());
                ((Map<String, Object>) rep.get("venue")).put("point", pt);
              }
            }
          }
        }
      }
      new_reps.add(rep);
    }

    if (new_reps.size() == 0)
      return null;
    else
      return new_reps;
  }
  /**
   * This function finds all event entities without coordinates and try to geocode each place.
   * @param index
   * @param type
   */
  public void searchFromTo(String index, String type) throws ExecutionException, InterruptedException, IOException {
    int from = 5000;
    int size = 100;
    int i = 0;
    boolean next = true;
    SearchResponse events = client.prepareSearch(index).setTypes(type).setFrom(from).setSize(size).setExplain(true).get();

    while (next) {
      for (SearchHit hit : events.getHits().getHits()) {
        String id = hit.getId();
        List<Map<String, Object>> reps = this.checkAndGeocode(hit.getSource());

        UpdateRequest updateRequest = new UpdateRequest();
        updateRequest.index("cognisearch_event_20170308");
        updateRequest.type("event_type");
        updateRequest.id(id);
        updateRequest.doc(jsonBuilder()
            .startObject()
            .field("performances", reps)
            .endObject());
        client.update(updateRequest).get();

      }
      i ++;
      from += i*size;
      events = client.prepareSearch(index).setTypes(type).setFrom(from).setSize(size).setExplain(true).get();

      //Break condition: No hits are returned
      if (events.getHits().getHits().length < size) {
        next = false;
      }
    }
  }
  /**
   * Searching in ES a specific field by value  AVsUIugY7_eJudBGJTLd
   * @param index
   * @param type
   * @param field
   * @param value
   * @return
   */
  public Map<String, Object> searchByField(String index, String type, String field, String value) {
    try {

      SearchResponse resp = client.prepareSearch(index).setTypes(type).setQuery(QueryBuilders.matchPhraseQuery(field, value)).execute().actionGet();
      SearchHit[] hits = resp.getHits().getHits();

      com.cogniteev.cognisearch.event.similarity.Utils simTools = new com.cogniteev.cognisearch.event.similarity.Utils(this.es_host, this.es_port, index, type);

      if (hits != null && hits.length > 0){
        if (this.similarityScoreForLabels(resp.getHits().getHits()[0].getSource(), value, field,  simTools) > this.THRESHOLD)
          return resp.getHits().getHits()[0].getSource();
        else
          return null;
      }
      else
        return null;
    }
    catch (Exception ex) {
      LOG.info("A error occured during ES interrogation : " + ex.getMessage());
      return null;
    }
  }

  /**
   * This function computes the matching score between a given string value and the corresponding Object with the Max score returned by ES
   * @param obj
   * @param value
   * @param field
   * @param simTools
   * @return
   */
  public double similarityScoreForLabels(Map<String, Object> obj, String value, String field, com.cogniteev.cognisearch.event.similarity.Utils simTools){
    double max = -10.0;

    switch (field) {
      case "labels" :
        Object labels = obj.get(field);

        // If we have only one label
        if (labels instanceof String) {
          max = simTools.softTFIDFscore(((String) labels).toLowerCase(), value.toLowerCase());
          break;
        }

        // Else it is an array
        for ( String s : (ArrayList<String>)labels){
          if (simTools.softTFIDFscore(s.toLowerCase(), value.toLowerCase()) > max)
            max = simTools.softTFIDFscore(s.toLowerCase(), value.toLowerCase());
        }
        break;
      case "city" :
        // The query is related to cities
        String city_name = (String)obj.get(field);
        if (simTools.softTFIDFscore(city_name.toLowerCase(), value.toLowerCase()) > max)
          max = simTools.softTFIDFscore(city_name.toLowerCase(), value.toLowerCase());

    }
    return max;
  }

  /**
   * Merging two list of String, the first one is coming form CRF so avoid some mistakes
   * @param l1
   * @param l2
   * @return
   */
  public List<String> mergeTwoListOfString(List<String> l1, List<String> l2) {

    if (l1 == null && l2 == null)
      return null;

    if (l1 == null && l2 != null)
      return l2;

    if (l1 != null && l2 == null)
      return l1;

    List<String> union = new ArrayList<>();

    for (String s: l1) {
      if (s.contains("."))
        continue;

      if (s.length() >= 3 && !s.contains("Quand"))
        union.add(s);
    }

    for (String s: l2)
      union.add(s);

    return this.deduplicate(union);
  }

  /**
   * This function removes duplicate elements from a List of String
   * @param l
   * @return
   */
  public List<String> deduplicate(List<String> l) {
    List<String> res = new ArrayList<>();

    for (String s : l) {
      if (!listContainsValue(res, s)){
        res.add(s);
      }
    }

    return res;
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
   * This extract the 100 first tokens of a string
   * @param rawContent
   * @return
   */
  public String topOfRawContent(String rawContent, int size) {
    String[] tokens = rawContent.split(" ");
    String res = "";
    int i = 0;
    while ( i< tokens.length && i < size) {
      res += tokens[i] + " ";
      i++;
    }
    return  res;
  }

  /**
   * Geocode a place using Address data Gouv
   * @param input
   * @return
   */
  public Place geocodeAddress(String input)
  {
    Place place = new Place();
    input = input.replace(" ", "%20");
    String url_string = "http://api-adresse.data.gouv.fr/search/?q=" + input;

    try {
      final String USER_AGENT = "Mozilla/5.0";
      URL url = new URL(url_string);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("User-Agent", USER_AGENT);
      conn.setRequestProperty("Accept", "application/json");

      int responseCode = conn.getResponseCode();

      if (conn.getResponseCode() != 200) {
        LOG.info("Error in server response");
        return null;
      }

      String output = "";
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (conn.getInputStream())));

      String line = "";

      while ((line = br.readLine()) != null) {
        output += line + "\n";
      }

      conn.disconnect();

      JSONParser parser = new JSONParser();
      //System.out.println(str);
      org.json.simple.JSONObject jsonObject = (org.json.simple.JSONObject) parser.parse(output);

      JSONArray features = (JSONArray) jsonObject.get("features");

      if (features.size() == 0)
        return  null;

      JSONObject first_result = (JSONObject) features.get(0);

      JSONObject properties = (JSONObject) first_result.get("properties");

      JSONObject geometry = (JSONObject) first_result.get("geometry");

      place.setAddress((String)properties.get("label"));

      JSONArray coordinates = (JSONArray) geometry.get("coordinates");

      place.setLon((Double) coordinates.get(0));
      place.setLat((Double) coordinates.get(1));


      return  place;
    }
    catch (Exception ex) {
      LOG.error("An error occured during adresse.data.gouv.fr Quering : " + ex.getMessage());
      return  null;
    }
  }

  /**
   * This function takes a place name and find it on GoogleMaps
   * @param place_name
   * @return Place occurrence ready to be indexed
   */
  public Place queryGoogle(String place_name) {
    place_name += " France";
    place_name = place_name.replace(" ", "%20");
    String url_string = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + place_name + "&key=" + api_key;

    try{
      final String USER_AGENT = "Mozilla/5.0";
      URL url = new URL(url_string);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("User-Agent", USER_AGENT);
      conn.setRequestProperty("Accept", "application/json");

      int responseCode = conn.getResponseCode();

      if (conn.getResponseCode() != 200) {
        LOG.info("Error in server response");
        return null;
      }

      String output = "";
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (conn.getInputStream())));

      String line = "";
      System.out.println("Output from Server .... \n");
      while ((line = br.readLine()) != null) {
        output += line + "\n";
      }

      conn.disconnect();

      JSONParser parser = new JSONParser();
      //System.out.println(str);
      org.json.simple.JSONObject jsonObject = (org.json.simple.JSONObject) parser.parse(output);


      // parse and get the object
      org.json.simple.JSONArray results = (org.json.simple.JSONArray) jsonObject.get("results");
      if (results.size() <= 0)
        return null;

      org.json.simple.JSONObject result = (JSONObject) results.get(0);
      System.out.println(result);

      Place p = new Place();

      p.setName(place_name.replace("%20", " ").replace(" France", ""));

      if (result.containsKey("formatted_address"))
        p.setAddress((String) result.get("formatted_address"));

      if (result.containsKey("geometry")) {
        org.json.simple.JSONObject geometry = (JSONObject) result.get(("geometry"));
        org.json.simple.JSONObject location = (JSONObject) geometry.get(("location"));

        p.setLat((double) location.get("lat"));
        p.setLon((double) location.get("lng"));
      }

      // We will know put this place in the index

      return p;

    } catch (MalformedURLException e) {
      e.printStackTrace();
      LOG.error("Malformed URL query " + e);
      return  null;
    } catch (ProtocolException e) {
      e.printStackTrace();
      LOG.error("Error during the query " + e);
      return  null;
    } catch (IOException e) {
      e.printStackTrace();
      LOG.error("Error during the query " + e);
      return  null;
    } catch (ParseException e) {
      e.printStackTrace();
      LOG.error("Error while parsing the JSON " + e);
      return null;
    }
  }

  /**
   * Index a place in elasticsearch
   * @param place
   * @param host_name
   * @param port_number
   * @return
   */
  public boolean index_place(Place place, String host_name, int port_number) {

    Client client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(host_name, port_number));

    Map<String, Object> point = new HashMap<>();
    GeoPoint coord = new GeoPoint(place.getLat(), place.getLon());

    point.put("labels", Arrays.asList(place.getName()));
    point.put("address", place.getAddress());
    point.put("uri", null);
    point.put("point", coord);

    try {
      IndexResponse response = client.prepareIndex("cognisearch_pois", "poi_type")
          .setSource(point)
          .get();

      LOG.info("Object successfully indexed " + point);
      return  true;
    }
    catch (Exception ex) {
      LOG.error("error while indexing : " + point);
      return  false;
    }

  }

  /**
   * Check if a String match a Regex
   * @param s
   * @param pattern
   * @return
   */
  public boolean stringMapRegex(String s, String pattern) {
    Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(s);
    if(m.matches())
      return true;

    return false;
  }

  /**
   * Index an event
   * @param event
   * @param index
   * @param type
   * @param host_name
   * @param port_number
   * @return
   */
  public boolean indexEvent(Event event, String index, String type, String host_name, int port_number) {

    try {
      Client client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(host_name, port_number));
        IndexResponse response = client.prepareIndex(index, type)
            .setSource(event.toMap())
            .get();
      LOG.info("Object successfully indexed " + event.toMap());
      System.out.println(event.toMap());
      return true;
    }
    catch (Exception ex){
      LOG.error("Error during object indexation " + ex.getMessage());
      return false;
    }

  }
  /**
   * Main for testing things
   * @param args
   */

  /**
   * This function takes an URL as parameters and returns the list of string where each one represent a line
   * @param location
   * @return
   * @throws IOException
   */
  public List<String> filetolist(URL location) throws IOException {
    List<String> tokens = new ArrayList<>();
    BufferedReader in = new BufferedReader(
        new InputStreamReader(location.openStream()));

    String inputLine;
    while ((inputLine = in.readLine()) != null)
      tokens.add(inputLine);
    in.close();
    return tokens;
  }


  public void addingEventOfGoogle(URL place_gazetier) throws IOException {
    List<String> places = this.filetolist(place_gazetier);

    for (String place : places) {
      Map p = this.searchByField("cognisearch_pois", "poi_type", "labels", place);

      if (p == null)
        System.out.println(place);
    }
  }

  /**
   * Calculate the centroid corresponding to a list of points
   * @param polygon
   * @return
   */
  public GeoPoint calculateCentroid(List<GeoPoint> polygon) {
    if (polygon == null || polygon.size() <= 0)
      return null;

    Double lat = 0.;
    Double lon = 0.;

    for (GeoPoint point : polygon) {
      lat += point.getLat();
      lon += point.getLon();
    }

    return new GeoPoint(lat / polygon.size(), lon / polygon.size());
  }

  /**
   * This function takes an URL as parameter and returns the rawHTML corresponding
   * @param url
   * @return
   * @throws IOException
   */
  public String getHTMLStringFromURL(String url) throws IOException {
    String htmlString = "";
    URL oracle = new URL(url);
    BufferedReader in = new BufferedReader(
        new InputStreamReader(oracle.openStream()));

    String inputLine;
    while ((inputLine = in.readLine()) != null)
      htmlString += inputLine + "\n";
    in.close();

    return htmlString;
  }

  /**
   * Reserve geocoding of POI using its coordinates
   * @param point
   * @return
   */
  public String reserseGeocoding(GeoPoint point) {
    if ( point == null )
      return "";

    String url_string = "http://api-adresse.data.gouv.fr/reverse/?lon=" + point.getLon() + "&lat=" + point.getLat();
    try {
      final String USER_AGENT = "Mozilla/5.0";
      URL url = new URL(url_string);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("User-Agent", USER_AGENT);
      conn.setRequestProperty("Accept", "application/json");

      int responseCode = conn.getResponseCode();

      if (conn.getResponseCode() != 200) {
        LOG.info("Error in server response");
        return "";
      }

      String output = "";
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (conn.getInputStream())));

      String line = "";

      while ((line = br.readLine()) != null) {
        output += line + "\n";
      }

      conn.disconnect();

      JSONParser parser = new JSONParser();
      //System.out.println(str);
      org.json.simple.JSONObject jsonObject = (org.json.simple.JSONObject) parser.parse(output);

      JSONArray features = (JSONArray) jsonObject.get("features");

      if (features.size() == 0)
        return  "";

      JSONObject first_result = (JSONObject) features.get(0);

      JSONObject properties = (JSONObject) first_result.get("properties");

      String add = (String) properties.get("street") + " " + (String) properties.get("postcode") + " " + (String) properties.get("city");

      return add;
    }
    catch (Exception ex) {
      LOG.error("An error occured during adresse.data.gouv.fr reverse geocoding : " + ex.getMessage());
      return  "";
    }

  }

  public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
    Utils tool = new Utils("localhost", 9300);

    // tool.searchFromTo("cognisearch_event_20170308", "event_type");

    // System.out.println(tool.searchByField("cognisearch_cities", "city_type", "city", "PARIS"));

    System.out.println(tool.getHTMLStringFromURL("https://www.mkyong.com/java/how-to-get-url-content-in-java/"));
  }
}
