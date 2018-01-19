package com.cogniteev.cognisearch.event.resources;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by armel on 19/01/17.
 */
public class CityResource {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CityResource.class);

  /**
   * Parse CSV and return a list of council's info
   * @param file
   * @return
   */
  public List<Map<String, Object>> parseCSV(String file) {
    List<Map<String, Object>>  elts =  new ArrayList<>();
    String line = "";
    String cvsSplitBy = "~";

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {

      while ((line = br.readLine()) != null) {
        // use comma as separator
        String[] cols = line.split(cvsSplitBy);
        Map<String, Object> elt = new HashMap<>();
        elt.put("cp", cols[0]);
        elt.put("city", cols[1]);

        String polygon_string = cols[2];
        // remove XML tags
        polygon_string = polygon_string.replace("\"<Polygon> <outerBoundaryIs> <LinearRing> <coordinates>", "");
        // Position of the ending of coordinates
        int pos = polygon_string.indexOf("</coordinates>");
        if ( pos >= 1)
          polygon_string = polygon_string.substring(0, pos-1);
        else {
          LOG.info("Error on : " + cols[1] + "     " + cols[2]);
          continue;
        }

        // List of string points
        String[] points_string = polygon_string.split(" ");

        // Build a list of maps
        List<Map<String, Double>> points = new ArrayList<>();

        for (String point_string : points_string) {
          String[] coords = point_string.split(",");
          Map<String, Double> point = new HashMap<>();

          point.put("lon", Double.parseDouble(coords[0]));
          point.put("lat", Double.parseDouble(coords[1]));

          points.add(point);

        }

        elt.put("polygon", points);
        elts.add(elt);
      }

      return elts;

    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }


  /**
   * Extract cities from POI and build Elasticsearch Index
   * @param file
   * @return
   */
  public List<Map<String, Object>> parseCSVForES(String file) {
    List<Map<String, Object>>  elts =  new ArrayList<>();
    String line = "";
    String cvsSplitBy = "~";

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {

      while ((line = br.readLine()) != null) {
        // use comma as separator
        String[] cols = line.split(cvsSplitBy);
        Map<String, Object> elt = new HashMap<>();
        elt.put("cp", cols[0]);
        elt.put("city", cols[1]);

        String polygon_string = cols[2];
        // remove XML tags
        polygon_string = polygon_string.replace("\"<Polygon> <outerBoundaryIs> <LinearRing> <coordinates>", "");
        // Position of the ending of coordinates
        int pos = polygon_string.indexOf("</coordinates>");
        if ( pos >= 1)
          polygon_string = polygon_string.substring(0, pos-1);
        else {
          LOG.info("Error on : " + cols[1] + "     " + cols[2]);
          continue;
        }

        // List of string points
        String[] points_string = polygon_string.split(" ");

        // Build a list of maps
        List<GeoPoint> points = new ArrayList<>();

        for (String point_string : points_string) {
          String[] coords = point_string.split(",");
          GeoPoint point = new GeoPoint(Double.parseDouble(coords[1]), Double.parseDouble(coords[0]));


          points.add(point);

        }

        elt.put("points", points);
        elts.add(elt);
      }

      return elts;

    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * This function extracts places coming from DBPedia and index in elasticsearch
   * @param filename
   * @param host_name
   * @param port_number
   * @return
   * @throws FileNotFoundException
   */
  public boolean index_cities(String filename, String host_name, int port_number) throws FileNotFoundException {
    Client client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(host_name, port_number));

    List<Map<String, Object>> cities = this.parseCSVForES(filename);
    try {

      for (Map city : cities) {
        IndexResponse response = client.prepareIndex("cognisearch_cities", "city_type")
            .setSource(city)
            .get();

        LOG.info("Object successfully indexed " + city);
      }
      return true;
    }
    catch (Exception ex){
      LOG.error("Error during object indexation " + ex.getMessage());
      return false;
    }

  }

  public void buildResource(String file_name, String outputFile, String dir) throws IOException {
    String dom = "<?xml version=\"1.0\"?>\n" +
        "\n" +
        "\n" +
        "<!DOCTYPE rdf:RDF [\n" +
        "    <!ENTITY owl \"http://www.w3.org/2002/07/owl#\" >\n" +
        "    <!ENTITY swrl \"http://www.w3.org/2003/11/swrl#\" >\n" +
        "    <!ENTITY swrlb \"http://www.w3.org/2003/11/swrlb#\" >\n" +
        "    <!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\" >\n" +
        "    <!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\" >\n" +
        "    <!ENTITY psys \"http://proton.semanticweb.org/protonsys#\" >\n" +
        "    <!ENTITY pext \"http://proton.semanticweb.org/protonext#\" >\n" +
        "    <!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" >\n" +
        "    <!ENTITY protege \"http://protege.stanford.edu/plugins/owl/protege#\" >\n" +
        "    <!ENTITY xsp \"http://www.owl-ontologies.com/2005/08/07/xsp.owl#\" >\n" +
        "]>\n" +
        "\n" +
        "\n" +
        "<rdf:RDF xmlns=\"http://www.owl-ontologies.com/cities.owl#\"\n" +
        "     xml:base=\"http://www.owl-ontologies.com/cities.owl\"\n" +
        "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
        "     xmlns:swrl=\"http://www.w3.org/2003/11/swrl#\"\n" +
        "     xmlns:protege=\"http://protege.stanford.edu/plugins/owl/protege#\"\n" +
        "     xmlns:psys=\"http://proton.semanticweb.org/protonsys#\"\n" +
        "     xmlns:xsp=\"http://www.owl-ontologies.com/2005/08/07/xsp.owl#\"\n" +
        "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n" +
        "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n" +
        "     xmlns:swrlb=\"http://www.w3.org/2003/11/swrlb#\"\n" +
        "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
        "     xmlns:pext=\"http://proton.semanticweb.org/protonext#\">\n" +
        "    \n" +
        "\n" +
        "\n" +
        "    <!-- \n" +
        "    ///////////////////////////////////////////////////////////////////////////////////////\n" +
        "    //\n" +
        "    // Classes\n" +
        "    //\n" +
        "    ///////////////////////////////////////////////////////////////////////////////////////\n" +
        "     -->\n";

    // build DOM
    List<Map<String, Object>> items = this.parseCSV(file_name);
    LOG.info(items + "");

    LOG.info("retrieved object size : " + items.size());

    // DO the job
    for (Map item : items) {
      String cp = (String)item.get("cp");
      String uri = "http://www.owl-ontologies.com/cities.owl#" + cp ;
      String city = (String) item.get("city");

      // The URI
      dom += "  <owl:Class rdf:about=\"" + uri + "\">\n";

      // The Label
      dom += "          <rdfs:label rdf:datatype=\"&xsd;string\">" + city.replace("&", " ") + "</rdfs:label> \n";

      // The CP
      dom += "          <rdfs:cp rdf:datatype=\"&xsd;string\">" + cp + "</rdfs:cp>\n";

      // Adding points
      List<Map<String, Double>> points = (List<Map<String, Double>>) item.get("polygon");

      for ( Map<String, Double> point : points) {
        dom += "          <rdfs:point rdf:datatype=\"&xsd;string\">" + point.get("lat") + "~" + point.get("lon") + "</rdfs:point>\n";
      }
      dom += "  </owl:Class>\n \n";
    }

    // Close the doc
    dom += "</rdf:RDF>";

    this.write_in_file(dir, outputFile, dom);
  }

  public void write_in_file(String dir_name, String file_name, String content) throws IOException {
    String encoding = "utf-8";
    File outputFile = new File(dir_name, file_name);
    FileOutputStream fos = new FileOutputStream(outputFile);
    BufferedOutputStream bos = new BufferedOutputStream(fos);
    OutputStreamWriter out;
    if(encoding == null) {
      out = new OutputStreamWriter(bos);
    }
    else {
      out = new OutputStreamWriter(bos, encoding);
    }

    out.write(content);

    out.close();
  }


  public static void main(String[] args) throws IOException {
    CityResource cR =  new CityResource();
    String dir_name = "/home/armel/Documents/These/Cognisearch Events Docs/Resources";
    String output = "cities.owl";
    // cR.buildResource("resources/Metropo.csv", output, dir_name);
    cR.index_cities("resources/Metropo.csv", "localhost", 9300);
  }
}
