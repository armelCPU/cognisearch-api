package com.cogniteev.cognisearch.event.resources;

import com.cogniteev.cognisearch.event.model.Place;
import com.cogniteev.cognisearch.event.pipe.Utils;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.*;
import java.util.*;

/**
 * Created by armel on 19/01/17.
 */
public class PlacesGoogle {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(PlacesGoogle.class);
  private Connection connection ;
  Statement stmt;
  // String api_key = "AIzaSyD_B84MC2GON5qYTOcRMTaTibfyr0Ce--U";
  String api_key = "AIzaSyBtX4YUEVdWBW069iuPOgVtpjeFbFpHFQk";

  Client client;

  /**
   * Instantiate the database
   * @param host
   * @param user
   * @param pass
   * @param database
   * @param port
   * @throws NamingException
   */
  public PlacesGoogle(String host, String user, String pass, String database, int port, String es_host, int es_port) throws NamingException {
    String url = "jdbc:mysql://" + host + ":" + port +"/" + database;
    LOG.info("Connection to the database " + database);
    try
    {
      connection = DriverManager.getConnection(url, user, pass);
      stmt = connection.createStatement();
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot connect the database!", e);
    }
    client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(es_host, es_port));

  }


  /**
   * Store a new place in the database
   * @param p
   * @param table
   * @return
   * @throws SQLException
   */
  public boolean storePlace(Place p, String table) throws SQLException {
    try{
      if ( this.getPlaceByName(table, p.getName()) != null) {
        LOG.info("The place " + p.getName() + " already exists in the index");
        return true;
      }

      // find if a place with this name exists
      String query = "INSERT INTO " + table + "(name, address, lat, lon)" +
          " VALUES (\"" + p.getName() +"\", \"" + p.getAddress() + "\", " + p.getLat() + ", " + p.getLon() + ")";
      LOG.info(query);
      stmt.execute(query);
      return true;
    }
    catch (Exception ex) {
      LOG.error("Error storing this place with error ! " + ex);
      return false;
    }
  }

  /**
   * Get all the place in the events place Table
   * @param table
   * @return
   * @throws SQLException
   */
  public List<Place> getQuery(String table) throws SQLException {
    List<Place> places = new ArrayList<>();
   try {
     ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);

     while (rs.next()) {
       Place p = new Place(rs);
       places.add(p);
     }
     return places;

   }
    catch (Exception ex) {
      LOG.error("Error while querying the resource ! " + ex);
      return null;
    }
  }

  /**
   * Get a place by name with an authorized error
   * @param table
   * @param name
   * @return
   */
  public Place getPlaceByNameWithApproximation(String table, String name) {
    try {
      String query = "SELECT * FROM " + table + " WHERE name LIKE \"%" + name + "%\"";
      System.out.println(query);
      ResultSet rs = stmt.executeQuery(query);

      if (rs.next()) {
        Place p = new Place(rs);
        return  p;
      }
      return  null;
    }
    catch (Exception ex) {
      LOG.error("Error while querying the resource ! " + ex);
      return null;
    }
  }

  /**
   * Get a place by name
   * @param table
   * @param name
   * @return
   */
  public Place getPlaceByName(String table, String name) {
    try {
      String query = "SELECT * FROM " + table + " WHERE name = \"" + name + "\"";
      ResultSet rs = stmt.executeQuery(query);

      if (rs.next()) {
        Place p = new Place(rs);
        return  p;
      }
      return  null;
    }
    catch (Exception ex) {
      LOG.error("Error while querying the resource ! " + ex);
      return null;
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

      System.out.println(p);

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
      LOG.error("Error while parsin the JSON " + e);
      return null;
    }
  }


  public static List<String> filetolist(URL location) throws IOException {
    List<String> tokens = new ArrayList<>();
    BufferedReader in = new BufferedReader(
        new InputStreamReader(location.openStream()));

    String inputLine;
    while ((inputLine = in.readLine()) != null)
      tokens.add(inputLine);
    in.close();
    return tokens;
  }

  /**
   * Index places
   * @param place_location
   */
  public void retrievePlaces(URL place_location) {
    Utils tool = new Utils("localhost", 9300);
    try {
      List<String> places = this.filetolist(place_location);

      for (String place : places) {

        //  Check if the place exists in Google maps resource which is store in the MySQL database
        if ( this.getPlaceByName("event_places", place) != null) {
          LOG.info("The place " + place + " already exists in the index");
          continue;
        }

        // Check, if this place is in the DBPedia resource before sending the query
        if ( tool.searchByField("cognisearch_pois", "poi_type", "labels", place) != null) {
          LOG.info("The place " + place + " already exists in the index");
          continue;
        }

        Place p = this.queryGoogle(place);
        Thread.sleep(10000);


        if (p == null)
          continue;

        // Else we have extract the place, we store it
        boolean well_stored = this.storePlace(p, "event_places");

        if ( !well_stored)
          LOG.warn("Error during the storage in the database of  " + place );
        else
          LOG.info("Place well store in the database " + place);

      }

    }
    catch (Exception ex) {
      LOG.error("Error while analyzing places " + ex.getMessage());
    }
  }

  /**
   * Searching in ES a specific field by value
   * @param host
   * @param port
   * @param index
   * @param type
   * @param field
   * @param value
   * @return
   */
  public Map<String, Object> searchByField(String host, int port, String index, String type, String field, String value) {
    try {

      SearchResponse resp = client.prepareSearch(index).setTypes(type).setQuery(QueryBuilders.matchPhraseQuery(field, value)).execute().actionGet();
      SearchHit[] hits = resp.getHits().getHits();

      if (hits != null && hits.length > 0)
        return resp.getHits().getHits()[0].getSource();
      else
        return null;
    }
    catch (Exception ex) {
      LOG.info("A error occured during ES interrogation : " + ex.getMessage());
      return null;
    }
  }

  /**
   * index all places properties stored in MySQL
   * @param table
   * @throws SQLException
   */
  public void indexGooglePlaces(String table) throws SQLException {

    List<Place> places = this.getQuery(table);

    for (Place place : places) {
      Map<String, Object> poi = searchByField("localhost", 9300, "cognisearch_pois", "poi_type", "labels", place.getName());

      if (poi != null) {
        LOG.info(place.getName() + "already exists in the index");
        continue;
      }


      Map<String, Object> point = new HashMap<>();
      GeoPoint coord = new GeoPoint(place.getLat(), place.getLon());

      point.put("labels", Arrays.asList(place.getName()));
      point.put("address", place.getAddress());
      point.put("uri", null);
      point.put("point", coord);

      IndexResponse response = client.prepareIndex("cognisearch_pois", "poi_type")
          .setSource(point)
          .get();

      LOG.info("Object successfully indexed " + point);
    }

  }

  public static void main(String[] args) throws NamingException, SQLException, MalformedURLException {
    String host = "localhost";
    int port = 3306;
    String user = "root";
    String pass = "";
    String database = "cognisearch";

    PlacesGoogle pl = new PlacesGoogle(host,user,pass, database, port, host, 9300);

    URL places = new File("/home/armel/Code/oncrawl_nutch/conf/GateRessources/AFGazetteer/AFPlace.lst").toURI().toURL();
    // pl.retrievePlaces(places);
    //System.out.println(pl.getPlaceByNameWithApproximation("event_places", "stade Saint"));
    pl.indexGooglePlaces("event_places");
  }
}
