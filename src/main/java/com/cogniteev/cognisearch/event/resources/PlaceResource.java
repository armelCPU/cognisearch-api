package com.cogniteev.cognisearch.event.resources;

import gate.creole.ResourceInstantiationException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by armel on 12/01/17.
 */
public class PlaceResource {
  List<String> prop;

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(PlaceResource.class);

  public PlaceResource() {
    prop = new ArrayList<>();

    prop.add("uri");
    prop.add("name");
    prop.add("label");
    prop.add("lat");
    prop.add("long");
    prop.add("surname");
    prop.add("address");

  }
  /**
   * This function read an HTML file and return the corresponding string
   * @param file_name
   * @return
   * @throws java.io.FileNotFoundException
   */
  public String readFile(String file_name) throws FileNotFoundException {
    String r = "";
    BufferedReader br = new BufferedReader(new FileReader(file_name));
    try
    {
      String line = br.readLine();
      while ( line!= null)
      {
        r += line;
        line = br.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return r;
  }

  /**
   * This function parse the HTML coming from dbpedia and returne
   * a list of Dictionaries with place pros
   * @param filename
   * @return
   * @throws FileNotFoundException
   */
  public List<Map<String, Object>> parse_dom_places(String filename) throws FileNotFoundException {
    List<Map<String, Object>> pois = new ArrayList<>();
    Document doc = Jsoup.parse(this.readFile(filename));
    Elements blocs = doc.select("tr");

    int pos = 0;

    for (org.jsoup.nodes.Element bloc : blocs) {
      if (pos == 0) {
        pos ++;
        continue;
      }
      /**
       * Foreach tr, get the props in this order
       * 1. URI
       * 2. name
       * 3. label
       * 4. lat
       * 5. long
       * 6. surname
       * 7. address when it exists
       */
      Elements props = bloc.children();
      Map<String, Object> place = new HashMap<>();
      int i= 0;

      for (Element prop : props) {
        place.put(this.prop.get(i), this.clean_label(prop.text()));
        i++ ;
      }

      pois.add(place);

    pos ++;
    }
    return pois;
  }

  /**
   * This function help to clean a string coming from wikipedia by
   * removing @... and quotes
   * @param input
   * @return
   */
  public String clean_label(String input)
  {
    int pos = input.indexOf("@");
    if (pos > 0)
      return input.substring(0, pos-1).replace("\"", "").replace("(", "").replace(")", "");

    return input.replace("\"", "").replace("(", "").replace(")", "");
  }

  /**
   * Parse a file and return a list of places with corresponding labels and properties
   * @param filename
   * @return
   * @throws FileNotFoundException
   */
  public List<Map<String, Object>> parse_to_dic(String filename) throws FileNotFoundException {
    List<Map<String, Object>> dics = new ArrayList<>();

    // Get raw POIS
    List<Map<String, Object>> pois = this.parse_dom_places(filename);

    // For each Poi,
    for (Map poi : pois) {
      if (! this.inPOIs(dics, poi)) {
        // This pois not already in the existing list, we build a new one
        Map<String, Object> place = new HashMap<>();

        place.put("uri", poi.get("uri"));

        /**
         * We need to build a geopoint here
         *
        place.put("lat", poi.get("lat"));
        place.put("long", poi.get("long"));
         */
        GeoPoint point = new GeoPoint(Double.parseDouble((String) poi.get("lat")), Double.parseDouble((String) poi.get("long")));
        place.put("point", point);

        if (poi.containsKey("address") && !poi.get("address").equals(""))
          place.put("address", poi.get("address"));

        // List of Labels, duplicate labels are automatically removed
        List<String> labels = new ArrayList();

        if (!poi.get("name").equals(""))
          if (! this.listContainsValue(labels, (String)poi.get("name")))
            labels.add((String) poi.get("name"));

        if (!poi.get("label").equals(""))
          if (! this.listContainsValue(labels, (String)poi.get("label")))
            labels.add((String) poi.get("label"));

        if (!poi.get("surname").equals(""))
          if (! this.listContainsValue(labels, (String)poi.get("surname")))
            labels.add((String) poi.get("surname"));


        // Add to the place
        place.put("labels", labels);

        dics.add(place);
      }
      else {
        // The POI already exists, update the label list
        int pos = this.positionInList(dics, poi);

        if (pos >= 0) {
          List<String> labels = (List<String>) dics.get(pos).get("labels");

          if (!poi.get("name").equals(""))
            if (!this.listContainsValue(labels, (String) poi.get("name")))
              labels.add((String) poi.get("name"));

          if (!poi.get("label").equals(""))
            if (! this.listContainsValue(labels, (String)poi.get("label")))
              labels.add((String) poi.get("label"));

          if (!poi.get("surname").equals(""))
            if (! this.listContainsValue(labels, (String)poi.get("surname")))
              labels.add((String) poi.get("surname"));

          // Add new labels
          dics.get(pos).put("labels", labels);
        }
        else
        {
          System.out.println("The POI is not in the list Why ?");
        }
      }
    }
    return  dics;
   }


  /**
   * This function extracts places coming from DBPedia and index in elasticsearch
   * @param filename
   * @param host_name
   * @param port_number
   * @return
   * @throws FileNotFoundException
   */
  public boolean index_places(String filename, String host_name, int port_number) throws FileNotFoundException {
    Client client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(host_name, port_number));

    List<Map<String, Object>> pois = this.parse_to_dic(filename);
    try {

      for (Map poi : pois) {
        IndexResponse response = client.prepareIndex("cognisearch_pois", "poi_type")
            .setSource(poi)
            .get();

        LOG.info("Object successfully indexed " + poi);
      }
      return true;
    }
    catch (Exception ex){
      LOG.error("Error during object indexation " + ex.getMessage());
      return false;
    }

  }

  /**
   * Build a place gazetter
   * @param dir_name
   * @param input_file
   * @param output_file
   * @throws IOException
   */
  public void buildGazetteer(String dir_name, String input_file, String output_file) throws IOException {
    List<Map<String, Object>> pois = this.parse_to_dic(input_file);
    String content = "";

    for (Map<String, Object> poi : pois) {
      List<String> labels = (List<String>) poi.get("labels");

      for (String label : labels) {
        content += label.replace("&", " ") + "\n";
      }
    }
    // Write in a the file
    this.write_in_file(dir_name, output_file, content);

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
   * Check if a Pois of dbpedia is in a list
   * @param pois
   * @param p
   * @return
   */
  private boolean inPOIs(List<Map<String, Object>> pois, Map<String, Object> p)
  {
    for ( Map poi : pois) {
      if (p.get("uri").equals(poi.get("uri")))
        return true;
    }
    return false;
  }

  /**
   * Return the position of an element in a list
   * @param pois
   * @param p
   * @return
   */
  private int positionInList(List<Map<String, Object>> pois, Map<String, Object> p) {
    int pos = -1;
    int cpt = 0;
    for ( Map poi : pois) {
      if (p.get("uri").equals(poi.get("uri")))
        return cpt;
      cpt ++;
    }
    return pos;
  }

  public String createDomFromMap(List<Map<String, Object>> dics) {
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
        "<rdf:RDF xmlns=\"http://www.owl-ontologies.com/produits.owl#\"\n" +
        "     xml:base=\"http://www.owl-ontologies.com/produits.owl\"\n" +
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
    for (Map item : dics) {

      String uri = (String) item.get("uri");

      // if the URI is complexe
      if (uri.contains("&"))
          continue;

      dom += "  <owl:Class rdf:about=\"" + item.get("uri") + "\">\n";

      // Introducing label
      List<String> labels = (List<String>) item.get("labels");
      for (String label : labels) {
        dom += "          <rdfs:label rdf:datatype=\"&xsd;string\">" + label.replace("&", " ") + "</rdfs:label> \n";
      }

      // address
      if (item.containsKey("address"))
        dom += "          <rdfs:address rdf:datatype=\"&xsd;string\">" + ((String)item.get("address")).replace("&", " ") + "</rdfs:address>\n";

      // coordinates
      dom += "          <rdfs:lat rdf:datatype=\"&xsd;string\">" + item.get("lat") + "</rdfs:lat>\n";

      dom += "          <rdfs:long rdf:datatype=\"&xsd;string\">" + item.get("long") + "</rdfs:long>\n";

      dom += "  </owl:Class>\n \n";
    }

    // Close the doc
    dom += "</rdf:RDF>";

    return dom;
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

  public static void main(String[] args) throws IOException, ResourceInstantiationException, ParseException, ClassNotFoundException {
    String file_name = "/home/armel/Documents/These/Cognisearch Events Docs/Resources/places_inria.html";
    String dir_name = "/home/armel/Documents/These/Cognisearch Events Docs/Resources";
    String output = "places.lst";
    PlaceResource pr = new PlaceResource();
    //List<Map<String, Object>> dics = pr.parse_to_dic(file_name);
    //String dom = pr.createDomFromMap(dics);
    //pr.write_in_file(dir_name, output, dom);
    pr.index_places(file_name, "localhost", 9300);
    // pr.buildGazetteer(dir_name, file_name, output);
  }
}
