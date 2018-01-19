package com.cogniteev.cognisearch.event.similarity;


import com.cogniteev.cognisearch.event.model.*;
import com.vividsolutions.jts.geom.*;
import com.wcohen.ss.SoftTFIDF;
import org.apache.commons.lang.WordUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by armel on 16/05/17.
 */
public class Utils {
  private SoftTFIDF cohenWorker;
  private int earth_radius = 6371; // In KM
  private int gamma = 20; // Place distance threshold in KM
  private int theta_date = 5;
  private int theta_hour = 60;
  private double buffer_width = 5; // Buffer distance for polygon's ring
  public static final String TEXT_COURT = "ShortText";
  public static final String CONCEPT = "Concept";
  private Client client;
  private String index;
  private String type;

  public String getOntology() {
    return ontology;
  }

  public void setOntology(String ontology) {
    this.ontology = ontology;
  }

  public String ontology = "resources/categoriesEvt.xml";


  public Utils(){
    cohenWorker = new SoftTFIDF();
    // Default Elasticsearch Client
    client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

  }

  public Utils(String es_host, int es_port, String index, String type){
    cohenWorker = new SoftTFIDF();
    client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(es_host, es_port));
    this.index = index;
    this.type = type;
  }

  /**
   * This function evaluate the similarity between two string given as parameter.
   * It uses the SoftTFIDF metric of
   * @param ch1
   * @param ch2
   * @return
   */
  public double softTFIDFscore(String ch1, String ch2){
    return cohenWorker.score(ch1, ch2) > 1 ? 1.00 : cohenWorker.score(ch1, ch2);

  }

  /**
   * Get two EventEntities from ES by using their IDs
   * @param id1
   * @param id2
   * @return
   */
  public List<EventEntity> getEventsFromES(String id1, String id2) {
    SearchResponse resp = client.prepareSearch(index).setTypes(type).setQuery(QueryBuilders.boolQuery().should(QueryBuilders.termQuery("_id", id1)).should(QueryBuilders.termQuery("_id", id2))).execute().actionGet();
    SearchHit[] hits = resp.getHits().getHits();

    if (hits == null || hits.length < 2)
      return null;

    // Else we have at least on result
    List<EventEntity> res = new ArrayList<EventEntity>();

    res.add(new EventEntity(hits[0].getSource(), hits[0].getId()));
    res.add(new EventEntity(hits[1].getSource(), hits[0].getId()));

    return res;
  }

  /**
   * GET two representations using their IDs
   * @param id1
   * @param id2
   * @return
   */
  public List<RepEntity> getRepsFromES(String id1, String id2) {
    SearchResponse resp = client.prepareSearch(index).setTypes(type).setQuery(QueryBuilders.boolQuery().should(QueryBuilders.termQuery("_id", id1)).should(QueryBuilders.termQuery("_id", id2))).execute().actionGet();
    SearchHit[] hits = resp.getHits().getHits();

    if (hits == null || hits.length < 2)
      return null;

    List<RepEntity> res = new ArrayList<>();

    res.add(new RepEntity(hits[0].getSource(), hits[0].getId()));
    res.add(new RepEntity(hits[1].getSource(), hits[1].getId()));

    return res;
  }

  /**
   * Get an event from index by id
   * @param id
   * @return
   */
  public EventEntity getEvent(String id) {
    SearchResponse resp = client.prepareSearch(index).setTypes(type).setQuery(QueryBuilders.matchQuery("_id", id)).setSize(1).execute().actionGet();

    SearchHit[] hits = resp.getHits().getHits();

    if (hits == null || hits.length < 1)
      return null;

    return new EventEntity(hits[0].getSource(), hits[0].getId());

  }

  /**
   * Get a representation from the index by Id
   * @param id
   * @return
   */
  public RepEntity getRepresentation(String id) {
    SearchResponse resp = client.prepareSearch(index).setTypes(type).setQuery(QueryBuilders.matchQuery("_id", id)).setSize(1).execute().actionGet();

    SearchHit[] hits = resp.getHits().getHits();

    if (hits == null || hits.length < 1)
      return null;

    return new RepEntity(hits[0].getSource(), hits[0].getId());
  }

  /**
   * Get All index elements
   * @return
   */
  public List<EventEntity> getAll(){
    SearchResponse resp = client.prepareSearch(index).setTypes(type).setQuery(QueryBuilders.matchAllQuery()).setSize(500).execute().actionGet();
    SearchHit[] hits = resp.getHits().getHits();

    // Else we have at least on result
    List<EventEntity> res = new ArrayList<EventEntity>();

    for (SearchHit hit : hits) {
      res.add(new EventEntity(hit.getSource(), hit.getId()));
    }

    return res;
  }
  /**
   * Get All index elements
   * @return
   */
  public List<RepEntity> getAllReps(){
    SearchResponse resp = client.prepareSearch(index).setTypes(type).setQuery(QueryBuilders.matchAllQuery()).setSize(500).execute().actionGet();
    SearchHit[] hits = resp.getHits().getHits();

    // Else we have at least on result
    List<RepEntity> res = new ArrayList<RepEntity>();

    for (SearchHit hit : hits) {
      res.add(new RepEntity(hit.getSource(), hit.getId()));
    }


    return res;
  }


  /**
   * Get all the clusters corresponding to a property
   * @param propName
   * @return
   */
  public List<Cluster> getPropClusters(String propName, String index, String type){
    SearchResponse resp = client.prepareSearch(index).setTypes(type).setQuery(QueryBuilders.matchAllQuery()).setSize(2).execute().actionGet();
    SearchHit[] hits = resp.getHits().getHits();

    // Else we have at least on result
    List<Cluster> res = new ArrayList<>();

    List<Map<String, Object>> dicsClusters = (List<Map<String, Object>>) (hits[0].getSource().get(propName) instanceof Map ? Arrays.asList(hits[0].getSource().get(propName)) : hits[0].getSource().get(propName));

    for(Map clus : dicsClusters){
      Cluster c = new Cluster(clus);
      res.add(c);
    }
    return  res;
  }

  /**
   * This function parses an OWL ontology with a deep of 4 in the hierarchy and returns the tree corresponding as a Map
   * @param owlFile
   * @return
   * @throws ParserConfigurationException
   * @throws IOException
   * @throws SAXException
   */
  public Map<String, Object> owlToMap(String owlFile) throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();

    List<String> alreadyParsed = new ArrayList<>();

    List<Map<String, String>> allNodesAndRelations=  new ArrayList<>();

    Document doc = builder.parse(owlFile);

    NodeList rootChilds = doc.getElementsByTagName("owl:Class");

    for ( int i =0 ; i < rootChilds.getLength(); i++) {
      Element child = (Element) rootChilds.item(i);

      // Get the concept associated to this node
      String concept = child.getAttributes().getNamedItem("rdf:about").getNodeValue();

      if ( alreadyParsed.contains(concept))
        continue;

      alreadyParsed.add(concept);

      Map<String, String> obj = new HashMap<>();

      obj.put("concept", concept);
      // Find if this concept has children
      NodeList parents =  child.getElementsByTagName("rdfs:subClassOf");
      if (parents != null && parents.getLength() > 0) {
        // This Node is a child of another one
        obj.put("parent", parents.item(0).getAttributes().getNamedItem("rdf:resource").getNodeValue());
      }

      allNodesAndRelations.add(obj);
    }

    Map<String, Object> tree = new HashMap<>();

    tree.put("key", "#");

    List<String> rootChildren = new ArrayList<>();

    for(Map elt : allNodesAndRelations) {
      if ( elt.get("parent") == null ) {
        rootChildren.add((String) elt.get("concept"));
        Map<String, Object> subtree = new HashMap<>();
        // Find this concept children
        List<String> conceptChildren = new ArrayList<>();
        for ( Map son : allNodesAndRelations) {
          // System.out.println((String) son.get("parent") + "     " + elt.get("concept"));
          if ( son.get("parent") != null && ((String) son.get("parent")).equalsIgnoreCase((String) elt.get("concept"))) {
            conceptChildren.add((String) son.get("concept"));
          }

          List<String> subConceptChildren = new ArrayList<>();
          Map<String, Object> subSubtree = new HashMap<>();

          for (Map subSon : allNodesAndRelations) {
            if ( subSon.get("parent") != null && ((String) subSon.get("parent")).equalsIgnoreCase((String) son.get("concept"))) {
              subConceptChildren.add((String) subSon.get("concept"));
            }

            List<String> subSubConceptChildren = new ArrayList<>();
            Map<String, Object> subSubSubtree = new HashMap<>();

            for (Map subSubSon : allNodesAndRelations) {
              if ( subSubSon.get("parent") != null && ((String)subSubSon.get("parent")).equalsIgnoreCase((String) subSon.get("concept"))) {
                subSubConceptChildren.add((String) subSubSon.get("concept"));
              }

              subSubSubtree.put("key", (String) subSon.get("concept"));
              subSubSubtree.put("children", subSubConceptChildren);
              subSubtree.put((String) subSon.get("concept"), subSubSubtree);
            }

            subSubtree.put("key", (String) son.get("concept"));
            subSubtree.put("children", subConceptChildren);
            subtree.put((String) son.get("concept"), subSubtree);
          }
        }

        subtree.put("key", (String) elt.get("concept"));
        subtree.put("children", conceptChildren);

        tree.put((String) elt.get("concept"), subtree);
      }
    }

    tree.put("children", rootChildren);


    return tree;
  }

  /**
   * Get all Concepts of an ontology as a List of maps
   * @param owlFile
   * @return
   * @throws ParserConfigurationException
   * @throws IOException
   * @throws SAXException
   */
  public List<Map<String, Object>> getNodes(String owlFile) throws ParserConfigurationException, IOException, SAXException
  {
    List<Map<String, Object>> nodes = new ArrayList<>();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(owlFile);

    NodeList rootChilds = doc.getElementsByTagName("owl:Class");
    for ( int i =0 ; i < rootChilds.getLength(); i++) {
      Element child = (Element) rootChilds.item(i);

      // Get the concept associated to this node
      String concept = child.getAttributes().getNamedItem("rdf:about").getNodeValue();

      // Get the first Label
      String name = null;

      if ( child.getElementsByTagName("rdfs:label").getLength() > 0){
        name = child.getElementsByTagName("rdfs:label").item(0).getTextContent();
      }
      else
        continue;

      Map<String, Object> node = new HashMap<>();
      node.put("uri", concept);
      node.put("name", name);

      nodes.add(node);
    }
    return  nodes;
  }

  /**
   * Get a node corresponding to a given concept
   * @param owlFile
   * @param concept
   * @return
   * @throws ParserConfigurationException
   * @throws IOException
   * @throws SAXException
   */
  public Map<String, Object> getNodeConcept(String owlFile, String concept)throws ParserConfigurationException, IOException, SAXException
  {
    Map<String, Object> node = new HashMap<>();

    List<Map<String, Object>> nodes = this.getNodes(owlFile);

    boolean found = false;

    for ( Map n : nodes) {
      String uri = (String) n.get("uri");
      if (uri.equalsIgnoreCase(concept)){
        found = true;
        node = n;
        break;
      }
    }

    if ( !found)
      return  null;

    return  node;
  }

  /**
   * Get all nodes of a map with all its labels
   * @param owlFile
   * @return
   * @throws ParserConfigurationException
   * @throws IOException
   * @throws SAXException
   */
  public List<Map<String, Object>> getNodesWithAllLabels(String owlFile) throws ParserConfigurationException, IOException, SAXException
  {
    List<Map<String, Object>> nodes = new ArrayList<>();

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(owlFile);

    NodeList rootChilds = doc.getElementsByTagName("owl:Class");
    for ( int i =0 ; i < rootChilds.getLength(); i++) {
      Element child = (Element) rootChilds.item(i);

      // Get the concept associated to this node
      String concept = child.getAttributes().getNamedItem("rdf:about").getNodeValue();

      // Get the first Label
      List<String> labels = new ArrayList<>();

      NodeList labelNodes = child.getElementsByTagName("rdfs:label");

      if (labelNodes.getLength() <= 0)
        continue;

      for( int j=0; j < labelNodes.getLength(); j++)
        labels.add(labelNodes.item(j).getTextContent());

      Map<String, Object> node = new HashMap<>();
      node.put("uri", concept);
      node.put("labels", labels);

      nodes.add(node);
    }

    return  nodes;
  }


  /**
   * This function get all the places of a type
   * @param place_index
   * @param place_type
   * @param target : 1 --> POI && 2 --> City
   * @return
   */
  public PlaceList getAllPlaces(String place_index, String place_type, int target){
    PlaceList res = new PlaceList();
    res.setResults(new ArrayList<ResultPlace>());

    int step = 1;
    int from = 0;
    int size = 1000;

    SearchResponse resp = client.prepareSearch(place_index).setTypes(place_type).setFrom(from).setSize(size).execute().actionGet();
    SearchHit[] hits = resp.getHits().getHits();

    while(hits.length > 0) {
      int i = 0;
      for (SearchHit hit : hits){
        ResultPlace p = new ResultPlace();
        Map<String, Object> obj = hit.getSource();

        // Place correspond to POI
        if ( target == 1) {

          List<String> labels = (List<String>) obj.get("labels");
          p.setId(i);

          p.setName(WordUtils.capitalize(labels.get(0)));
          p.setUri((String) obj.get("uri"));

          Map<String, Object> pt = (Map<String, Object>)obj.get("point");

          Resultpoint resPt = new Resultpoint();

          resPt.setLat((Double)pt.get("lat"));
          resPt.setLon((Double)pt.get("lon"));

          p.setPoints(Arrays.asList(resPt));
        }

        // Place is a city
        if ( target == 2) {
          p.setId(i);

          p.setName(WordUtils.capitalize((String) obj.get("city")));

          List<Resultpoint> points = new ArrayList<>();

          List<Map<String, Double>> pts = (List<Map<String, Double>>) obj.get("points");

          for( Map pt : pts) {
            Resultpoint resPt = new Resultpoint();

            resPt.setLat((Double)pt.get("lat"));
            resPt.setLon((Double)pt.get("lon"));

            points.add(resPt);
          }

          p.setPoints(points);
        }

        res.getResults().add(p);

        i ++;
      }

      // Iterate
      from += step*size;
      resp = client.prepareSearch(place_index).setTypes(place_type).setQuery(QueryBuilders.matchAllQuery()).setSize(size).setFrom(from).execute().actionGet();
      hits = resp.getHits().getHits();
    }

    return res;
  }

  /**
   * This function parses performers index and returns all their names an URI for the search
   * @param perf_index
   * @param perf_type
   * @return
   */
  public  CatList getAllPerformers(String perf_index, String perf_type){
    CatList res = new CatList();
    res.setResults(new ArrayList<ResultCat>());

    int step = 1;
    int from = 0;
    int size = 1000;

    SearchResponse resp = client.prepareSearch(perf_index).setTypes(perf_type).setFrom(from).setSize(size).execute().actionGet();
    SearchHit[] hits = resp.getHits().getHits();
    int i = 0;
    while(hits.length > 0) {

      for (SearchHit hit : hits) {
        ResultCat p = new ResultCat();

        Map<String, Object> obj = hit.getSource();
        List<String> labels = (List<String>) obj.get("labels");
        p.setId(i+1);
        p.setName(labels.get(0));

        res.getResults().add(p);
        i++;
      }
      from += step*size;
      resp = client.prepareSearch(perf_index).setTypes(perf_type).setFrom(from).setSize(size).execute().actionGet();
      hits = resp.getHits().getHits();
    }

    // Adding Jeff Panacloc hard-coded
    ResultCat p = new ResultCat();
    p.setId(i+1);
    p.setName("Jeff Panacloc");
    p.setUri("https://en.wikipedia.org/wiki/Jeff_Panacloc");
    res.getResults().add(p);

    return res;
  }
  /**
   * This function returns the dept of a concept in an ontology
   * Root node depth is 0
   * -1 is returned if the concept doesn't exist
   * @param concept
   * @param tree
   * @return
   */
  public double depth(String concept, Map<String, Object> tree) {
    double depthness = 1;
    boolean found = false;

    if (concept.equalsIgnoreCase("#"))
      return 0;
    List<String> rootChildren = (List<String>) tree.get("children");
    if (rootChildren.contains(concept))
      return depthness;

    // second level of the tree
    Iterator listIt = rootChildren.iterator();
    while (listIt.hasNext() && !found){
      String rootChild = (String) listIt.next();
      Map<String, Object> subTree = (Map<String, Object>) tree.get(rootChild);
      List<String> subChildren = (List<String>) subTree.get("children");
      if ( subChildren.contains(concept))
      {
        found = true;
        depthness = 2;
        break;
      }

      // We continue with the next level
      Iterator subListIt = subChildren.iterator();
      while (subListIt.hasNext() && !found){
        String subChild = (String) subListIt.next();

        Map<String, Object> subSubTree = (Map<String, Object>) subTree.get(subChild);
        List<String> subSubChildren = (List<String>) subSubTree.get("children");

        if ( subSubChildren.contains(concept))
        {
          found = true;
          depthness = 3;
          break;
        }

        // We continue with the next level
        Iterator subSubListIt = subSubChildren.iterator();
        while (subSubListIt.hasNext() && !found){
          String subSubChild = (String) subSubListIt.next();

          Map<String, Object> subSubSubSubTree = (Map<String, Object>) subSubTree.get(subSubChild);
          List<String> subSubSubSubChildren = (List<String>) subSubSubSubTree.get("children");

          if ( subSubSubSubChildren.contains(concept))
          {
            found = true;
            depthness = 4;
            break;
          }
        }
      }

    }

    return !found ? -1 : depthness;
  }

  /**
   * This function finds the lower common ancestor of two ontology concepts.
   * We browse the tree ans we save all the paths of each node from the root
   *
   * If the concept is not in the ontology we return null
   * We return # if it is the root
   * @param concept1
   * @param concept2
   * @return
   */
  public String lower_common_ancestor(String concept1, String concept2, Map<String, Object> tree) {

    if ( concept1 != null && concept2 != null && concept2.equalsIgnoreCase(concept1))
      return concept1;

    List<String> rootChildren = (List<String>) tree.get("children");
    List<String> concept1_lca = new ArrayList<>();
    List<String> concept2_lca = new ArrayList<>();
    String lca = "#";

    boolean foundConcept1 = false;
    boolean foundConcept2 = false;

    // Root is parent of everyone if it exists
    concept1_lca.add("#");
    concept2_lca.add("#");
    if (rootChildren.contains(concept1) && rootChildren.contains(concept2))
      return "#";

    if ( rootChildren.contains(concept1))
      foundConcept1 = true;

    if ( rootChildren.contains(concept2))
      foundConcept2 = true;


    Iterator listIt = rootChildren.iterator();
    while (listIt.hasNext()) {
      String rootChild = (String) listIt.next();
      Map<String, Object> subTree = (Map<String, Object>) tree.get(rootChild);
      List<String> subChildren = (List<String>) subTree.get("children");


      if (subChildren.contains(concept1) && subChildren.contains(concept2)){
        concept1_lca.add((String) subTree.get("key"));
        concept2_lca.add((String) subTree.get("key"));
        foundConcept1 = true;
        foundConcept2 = true;
        break;
      }

      if ( subChildren.contains(concept1)){
        concept1_lca.add((String) subTree.get("key"));
        foundConcept1 = true;

        if ( foundConcept2)
          break;
      }

      if ( subChildren.contains(concept2)) {
        concept2_lca.add((String) subTree.get("key"));
        foundConcept2 = true;

        if (foundConcept1 )
          break;
      }

      // We continue with the next level
      Iterator subListIt = subChildren.iterator();

      while (subListIt.hasNext()) {
        String subChild = (String) subListIt.next();

        Map<String, Object> subSubTree = (Map<String, Object>) subTree.get(subChild);
        List<String> subSubChildren = (List<String>) subSubTree.get("children");

        if (subSubChildren.contains(concept1) && subSubChildren.contains(concept2)){
          concept1_lca.add((String) subTree.get("key"));
          concept1_lca.add((String) subSubTree.get("key"));

          concept2_lca.add((String) subTree.get("key"));
          concept2_lca.add((String) subSubTree.get("key"));

          foundConcept1 = true;
          foundConcept2 = true;

          break;
        }

        if (subSubChildren.contains(concept1)){
          concept1_lca.add((String) subTree.get("key"));
          concept1_lca.add((String) subSubTree.get("key"));
          foundConcept1 = true;

          if ( foundConcept2)
            break;
        }

        if (subSubChildren.contains(concept2)){
          concept2_lca.add((String) subTree.get("key"));
          concept2_lca.add((String) subSubTree.get("key"));
          foundConcept2 = true;

          if (foundConcept1 )
            break;
        }

        // We continue with the next level
        Iterator subSubListIt = subSubChildren.iterator();

        while (subSubListIt.hasNext()) {
          String subSubChild = (String) subSubListIt.next();

          Map<String, Object> subSubSubTree = (Map<String, Object>) subSubTree.get(subSubChild);

          if (subSubSubTree == null)
            continue;

          List<String> subSubSubChildren = (List<String>) subSubSubTree.get("children");

          if (subSubSubChildren.contains(concept1) && subSubSubChildren.contains(concept2)){
            concept1_lca.add((String) subTree.get("key"));
            concept1_lca.add((String) subSubTree.get("key"));
            concept1_lca.add((String) subSubSubTree.get("key"));

            concept2_lca.add((String) subTree.get("key"));
            concept2_lca.add((String) subSubTree.get("key"));
            concept2_lca.add((String) subSubSubTree.get("key"));

            foundConcept1 = true;
            foundConcept2 = true;

            break;
          }

          if (subSubSubChildren.contains(concept1)){
            concept1_lca.add((String) subTree.get("key"));
            concept1_lca.add((String) subSubTree.get("key"));
            concept1_lca.add((String) subSubSubTree.get("key"));
            foundConcept1 = true;

            if ( foundConcept2)
              break;
          }

          if (subSubSubChildren.contains(concept2)){
            concept2_lca.add((String) subTree.get("key"));
            concept2_lca.add((String) subSubTree.get("key"));
            concept2_lca.add((String) subSubSubTree.get("key"));
            foundConcept2 = true;

            if (foundConcept1 )
              break;
          }

        }

      }
    }

    // Case where one of the concept is already in the other one ancestor
    if ( concept2_lca.contains(concept1))
      return concept1;

    if ( concept1_lca.contains(concept2))
      return concept2;


    int i, j;
    boolean cont = true;

    lca = "#";
    for ( i=1, j=1; i< concept1_lca.size() && j<concept2_lca.size(); i++, j++) {
      if (concept1_lca.get(i).equalsIgnoreCase(concept2_lca.get(j))) {
        lca = concept1_lca.get(i);
        break;
      }
    }

    return lca;
  }

  /**
   * This function compute the Wu & Palmer similarity score for two concepts
   * It returns -1 if there is concept doesn't exist
   * @param concept1
   * @param concept2
   * @return
   */
  public double wuPalmerScore(String concept1, String concept2) throws IOException, SAXException, ParserConfigurationException {
    Map<String, Object> tree = this.owlToMap(this.ontology);

    String lca = this.lower_common_ancestor(concept1, concept2, tree);


    if (lca == null)
      return  -1;

    double depth_lca = this.depth(lca, tree);
    double depth_concept1 = this.depth(concept1, tree);
    double depth_concept2 = this.depth(concept2, tree);

    if (depth_concept1 < 0 || depth_concept2 < 0 || depth_lca < 0)
      return -1.0;

    return (2*depth_lca)/(depth_concept1 + depth_concept2);
  }


  /**
   * This function computes the
   * @param point1
   * @param point2
   * @return
   */
  public double geoPointSimilarity(GeoPoint point1, GeoPoint point2 )
  {
    double dist = this.haversineDist(point1, point2);
    if (dist > this.gamma)
      return 0;
    else
      return (1-(dist/this.gamma));
  }

  /**
   * This function computes the Haversine distance between two given points
   * @param point1
   * @param point2
   * @return
   */
  private double haversineDist(GeoPoint point1, GeoPoint point2){
    double latDistance = Math.toRadians(Math.abs(point2.getLat() - point1.getLat()));
    double lonDistance = Math.toRadians(Math.abs(point2.getLon() - point1.getLon()));

    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(point1.getLat())) * Math.cos(Math.toRadians(point2.getLat()))
        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return this.earth_radius*c;
  }

  /**
   * This function converts a list of geo-points corresponding to a polygon in a list of coordinates
   * @param polygon
   * @return
   */
  private Coordinate[] createCoordinates(List<GeoPoint> polygon){
    Coordinate[] coords = null;

    if (polygon.get(0).getLat() == polygon.get(polygon.size()-1).getLat() && polygon.get(0).getLon() == polygon.get(polygon.size()-1).getLon()){
      coords = new Coordinate[polygon.size()];
      int i = 0;
      for ( GeoPoint point : polygon) {
        coords[i] = new Coordinate(point.getLat(), point.getLon());
        i++;
      }
    }
    else{
      coords = new Coordinate[polygon.size()+1];
      int i = 0;
      for ( GeoPoint point : polygon) {
        coords[i] = new Coordinate(point.getLat(), point.getLon());
        i++;
      }
      coords[polygon.size()] = new Coordinate(polygon.get(0).getLat(), polygon.get(0).getLon());
    }
    return  coords;
  }

  /**
   * This function computes the similarity between two polygonal shapes
   * @param poly1
   * @param poly2
   * @return
   */
  public double geoPolygonSimilarity(List<GeoPoint> poly1, List<GeoPoint>poly2){
    Polygon p1 = new GeometryFactory().createPolygon(this.createCoordinates(poly1));
    Polygon p2 = new GeometryFactory().createPolygon(this.createCoordinates(poly2));

    /*
     * If the two polygon are disjoint, no similarity possible
     */
    if(p1.disjoint(p2))
      return 0.00;

    Polygon inter = null;
    /**
     * TODO
     * if the two polygons are very closed by not equal, an TopologyException is throw out
     */
    try{
      inter = (Polygon) p1.intersection(p2);
    }
    catch (Exception ex){
      // Overlays with a little error
      return 0.95;
    }


    Polygon union = (Polygon) p1.union(p2);

    if ( union.getArea() == 0)
      return  -1.00;

    return inter.getArea()/union.getArea();
  }

  /**
   * This function compute the max distance between bound of a polygon and a given point
   * @param p
   * @param poly
   * @return
   */
  private double maxDistancePointShapes(GeoPoint p, List<GeoPoint> poly){
    double max = 0.0;
    for( GeoPoint point : poly) {
      if ( this.haversineDist(p, point) > max)
        max = this.haversineDist(p, point);
    }

    return  max;
  }

  /**
   *
   * @param p
   */
  private void printPolygon(Coordinate[] p) {
    for ( Coordinate c :p){
      System.out.println(c);
    }
  }

  /**
   * Convert a polygon to a list of GeoPoints
   * @param p
   * @return
   */
  private List<GeoPoint> polygonToListOfGeo(Polygon p){
    List<GeoPoint> l =  new ArrayList<>();

    for( Coordinate c : p.getCoordinates()){
      GeoPoint pt = new GeoPoint(c.getOrdinate(0), c.getOrdinate(1));

      l.add(pt);
    }

    return l;
  }

  /**
   * This function compute the similarity between a point and a polygon
   * @param p
   * @param poly
   * @return
   */
  public double pointPolygonSimilarity(GeoPoint p, List<GeoPoint> poly){

    Point point = new GeometryFactory().createPoint(new Coordinate(p.getLat(), p.getLon()));
    Polygon polygon = new GeometryFactory().createPolygon(this.createCoordinates(poly));

    Polygon buffer = (Polygon) polygon.buffer(this.buffer_width);
    //this.printPolygon(buffer.getCoordinates());

    Coordinate[] coords = buffer.getEnvelope().getCoordinates();

    double d1 = this.haversineDist(this.makeGeopointFromCoordinate(coords[0]), this.makeGeopointFromCoordinate(coords[1]));
    double d2 = this.haversineDist(this.makeGeopointFromCoordinate(coords[1]), this.makeGeopointFromCoordinate(coords[2]));

    double ajust_coef = Math.pow(Math.min(d1, d2)/Math.max(d1, d2), 2);

    if ( ! buffer.contains(point))
      return 0.0;

    double distToCenter = this.haversineDist(p, this.calculateCentroid(this.polygonToListOfGeo(buffer)));

    double rho = this.maxDistancePointShapes(this.calculateCentroid(this.polygonToListOfGeo(buffer)), this.polygonToListOfGeo(buffer));

    return 1-(ajust_coef)*(distToCenter/rho);
  }

  private GeoPoint makeGeopointFromCoordinate(Coordinate coord){
    return new GeoPoint(coord.getOrdinate(0), coord.getOrdinate(1));
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
   * This function computes similarity between dates
   * @param d1
   * @param d2
   * @return
   */
  public double dateSimilarity(SimpleDate d1, SimpleDate d2){
    if (d1.dateInDays() < 0 || d2.dateInDays() < 0)
      return 0.0;


    long diff = Math.abs(d1.dateInDays() - d2.dateInDays());
    if ( diff > this.theta_date)
      return 0.0;

    return (1- (double)diff/theta_date);
  }

  /**
   * This function computes similarity between hours
   * @param h1
   * @param h2
   * @return
   */
  public double hourSimilarity(SimpleHour h1, SimpleHour h2){
    long diff = Math.abs(h1.getTimeInMinutes() - h2.getTimeInMinutes());

    if ( diff > this.theta_hour)
      return 0.0;

    return (1- (double)diff/theta_hour);
  }

  /**
   * This function compute the similarity between two set of elements
   * This function is based on works of Halkidi et al. 2003
   * @param l1
   * @param l2
   * @param type : Type of elements of the sets
   * @return
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  public double ensembleSimilarity(List<String> l1, List<String> l2, String type) throws ParserConfigurationException, SAXException, IOException {
    double sum_left = 0.0;

    // Left Part

    int nb_gauche = 0;
    int nb_droite = 0;

    for(Object elt : l1){
      double max_UV = 0.0;
      for( Object second : l2) {
        double score;
        switch (type) {
          case Utils.TEXT_COURT :
            score= this.softTFIDFscore((String)elt, (String)second);

            if( score >= 0 && score > max_UV) {
              max_UV = score;
              nb_gauche ++;
            }

          case Utils.CONCEPT :
            score = this.wuPalmerScore((String)elt, (String)second);

            if( score >= 0 && score > max_UV) {
              max_UV = score;
              nb_gauche ++;
            }

        }

      }
      sum_left += max_UV;
    }

    // RIGHT PART
    double sum_right = 0.0;

    for(Object elt : l2){
      double max_UV = 0.0;
      for( Object second : l1) {
        double score;
        switch (type) {
          case Utils.TEXT_COURT :
            score= this.softTFIDFscore((String)elt, (String)second);
            if (score < 0)
              continue;

            if( score >= 0 && score > max_UV) {
              max_UV = score;
              nb_droite ++;
            }

          case Utils.CONCEPT :
            score = this.wuPalmerScore((String)elt, (String)second);
            if (score < 0)
              continue;

            if( score >= 0 && score > max_UV) {
              max_UV = score;
              nb_droite ++;
            }
        }

      }
      sum_right += max_UV;
    }

    // Combination
    if ( nb_droite == 0 && nb_gauche == 0)
      return  0.0;

    if (nb_gauche == 0 && nb_droite != 0)
      return sum_right/nb_droite;

    if (nb_droite == 0 && nb_gauche == 0)
      return sum_left/nb_gauche;

    return  0.5*(sum_left/nb_gauche + sum_right/nb_droite);
  }


  /**
   * Parse a test file and return all line as an array of string
   * @param file
   * @return
   */
  public ArrayList<String[]>  parseCsv(String file){
    ArrayList<String[]> lines = new ArrayList<>();
    String line = "";
    String cvsSplitBy = ",";

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {

      while ((line = br.readLine()) != null) {
        // use comma as separator
        String[] lineItems = line.split(cvsSplitBy);
        lines.add(lineItems);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines;
  }


  public String printArray(String[] tab){
    String res = "";
    for(int i=0; i < tab.length -1; i++)
      res += (tab[i] + "\t");
    res += (tab[tab.length - 1] + "\n");

    return res;
  }


  public static void main(String[] args) throws Exception {
    Utils tools = new Utils();

    tools.parseCsv("resources/testset.csv");
    /*
    TEST STRING SIMILARITY
    */
    String ch1 = "Main Square";
    String ch2 = "Main Square Festival 2017";

    System.out.println(tools.softTFIDFscore(ch1, ch2));
    /* */


    /*
    TEST SEMANTIC SIMILARITY
      */
    String xmlFile = "resources/categoriesEvt.xml";
    /*
    Map<String, Object> tree = tools.owlToMap(xmlFile);
    System.out.println(tools.wuPalmerScore("http://www.owl-ontologies.com/categoriesEvt.owl#Variete", "http://www.owl-ontologies.com/categoriesEvt.owl#FestivalDanse"));
    */
    System.out.println(tools.getNodeConcept(xmlFile, "http://www.owl-ontologies.com/categoriesEvt.owl#Humour"));


    /*
    TEST GEO POINT SIMILARITY

    GeoPoint point1 = new GeoPoint(48.837789,-0.57918);
    GeoPoint point2 = new GeoPoint(44.8649497,-0.6209231);

    System.out.println(tools.geoPointSimilarity(point1, point2));
    */

    /*
    TEST GEO POLYGON SIMILARITY

    List<GeoPoint> p1 = new ArrayList<>();
    p1.add(new GeoPoint(0.0575,0.054));
    p1.add(new GeoPoint(0.42,10.45));
    p1.add(new GeoPoint(10,10));
    p1.add(new GeoPoint(10.255,0.247));
    p1.add(new GeoPoint(0.0575,0.054));

    List<GeoPoint> p2 = new ArrayList<>();
    p2.add(new GeoPoint(5.1244, 5.2148));
    p2.add(new GeoPoint(15.555,5.587));
    p2.add(new GeoPoint(15.155,15.587));
    p2.add(new GeoPoint(5.155,15.587));
    p2.add(new GeoPoint(5.1244, 5.2148));

    System.out.println(tools.geoPolygonSimilarity(p1, p2));
    */

    /*
      TEST POINT - POLYGON SIMILARITY

    List<GeoPoint> p2 = new ArrayList<>();
    p2.add(new GeoPoint(5.1244, 5.2148));
    p2.add(new GeoPoint(15.555,5.587));
    p2.add(new GeoPoint(15.155,15.587));
    p2.add(new GeoPoint(5.155,15.587));
    p2.add(new GeoPoint(5.1244, 5.2148));

    GeoPoint p = new GeoPoint(18.00, 18.58);

    System.out.println(tools.pointPolygonSimilarity(p, p2));
    */


    /*
     * TEST DATE SIMILARITY

    SimpleDate d1 = new SimpleDate(14, 5, 2017);
    SimpleDate d2 = new SimpleDate(15, 5, 2017);
    System.out.println(tools.dateSimilarity(d1, d2));
    */

    /*
     * TEST HOUR SIMILARITY

    SimpleHour h1 = new SimpleHour(12, 40);
    SimpleHour h2 = new SimpleHour(13, 39);
    System.out.println(tools.hourSimilarity(h1, h2));
    */

    /*
     * TEST SEMANTIC SIMILARITY

    List<String> set_1 = Arrays.asList("http://www.owl-ontologies.com/categoriesEvt.owl#FestivalDanse");
    List<String> set_2 = Arrays.asList("http://www.owl-ontologies.com/categoriesEvt.owl#Football");

    System.out.println(tools.ensembleSimilarity(set_1, set_2, "Concept"));
    */
  }
}
