package com.cogniteev.cognisearch.event.repSimilarity;

import au.com.bytecode.opencsv.CSVReader;
import com.cogniteev.cognisearch.event.model.Cluster;
import com.cogniteev.cognisearch.event.model.RepEntity;
import com.cogniteev.cognisearch.event.model.SimpleDate;
import com.cogniteev.cognisearch.event.model.SimpleHour;
import com.cogniteev.cognisearch.event.pipe.ElasicsearchObject;
import com.cogniteev.cognisearch.event.similarity.Utils;
import org.elasticsearch.common.geo.GeoPoint;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by armel on 27/07/17.
 */
public class Clustering {
  private Utils tools;
  private List<RepEntity> reps;
  private List<Cluster> venue_clusters;
  private List<Cluster> date_clusters;
  private List<Cluster> hour_clusters;
  private ElasicsearchObject es;
  private int nb_EN = 0;

  private final double VENUE_THRESHOLD = 0.80;
  private final double DATE_THRESHOLD = 0.9;
  private final double HOUR_THRESHOLD = 0.90;


  public Clustering (String es_host, int es_port, String index, String type){
    tools =  new Utils(es_host, es_port, index, type);
    es = new ElasicsearchObject(es_host, es_port);
  }

  /**
   * Load Clusters data from ElasticSearch
   */
  public void loadRepClusters(){
    this.venue_clusters = tools.getPropClusters("venue", "clusters_representations", "clusters_representation");
    this.date_clusters = tools.getPropClusters("date", "clusters_representations", "clusters_representation");
    this.hour_clusters = tools.getPropClusters("hour", "clusters_representations", "clusters_representation");


    System.out.println("Nb events : " + this.nb_EN);
    System.out.println("Nb venue clusters : " + this.venue_clusters.size());
    System.out.println("Nb date clusters : " + this.date_clusters.size());
    System.out.println("Nb hour clusters : " + this.hour_clusters.size());

  }

  public void printClusters( List<Cluster> c){
    for ( Cluster c1 : c)
      System.out.println(c1);

    System.out.println();
  }

  /**
   * Load the index
   */
  public void loadIndex(){
    this.reps = this.tools.getAllReps();
    this.nb_EN = this.reps.size();
  }

  /**
   * Build venues clusters
   * @return
   */
  public List<Cluster> buildVenueClusters(){
    this.loadIndex();


    List<Cluster> clusters = new ArrayList<>();

    Cluster firstCluster = new Cluster();
    firstCluster.setName("Cluster 1");

    if ( this.reps.get(0).getVenue() instanceof List) {
      // calculate the centroid of the polygon and set the center of the cluster
      firstCluster.setCentroid(tools.calculateCentroid((List)this.reps.get(0).getVenue()));
    }
    else{
      firstCluster.setCentroid(this.reps.get(0).getVenue());
    }

    firstCluster.getElements().add(this.reps.get(0).getId());

    clusters.add(firstCluster);

    for (int i=1; i < this.reps.size(); i++) {
      if ( this.reps.get(i).getVenue() == null)
        continue;

      boolean found = false;
      GeoPoint center =null;

      // If this venue is a polygon calculate the centroid
      if ( this.reps.get(i).getVenue() instanceof List)
        center = tools.calculateCentroid((List)this.reps.get(i).getVenue());
      else
        center = (GeoPoint)this.reps.get(i).getVenue();

      for ( Cluster clust : clusters){
        if ( tools.geoPointSimilarity(center, (GeoPoint)clust.getCentroid()) > VENUE_THRESHOLD){
          clust.getElements().add(this.reps.get(i).getId());

          found = true;
          break;
        }
      }

      if ( !found) {
        Cluster newCluster = new Cluster();
        newCluster.setName("Cluster " + (i + 1));
        newCluster.setCentroid(center);
        newCluster.getElements().add(this.reps.get(i).getId());

        clusters.add(newCluster);
      }
    }

    return clusters;
  }

  /**
   * Build dates Cluster
   * @return
   */
  public List<Cluster> buildDateClusters(){
    this.loadIndex();


    List<Cluster> clusters = new ArrayList<>();

    Cluster firstCluster = new Cluster();
    firstCluster.setName("Cluster 1");
    firstCluster.setCentroid(this.reps.get(0).getDate());

    firstCluster.getElements().add(this.reps.get(0).getId());

    clusters.add(firstCluster);

    for (int i=1; i < this.reps.size(); i++) {
      if ( this.reps.get(i).getDate() == null)
        continue;

      boolean found = false;

      for ( Cluster clust : clusters){
        if ( tools.dateSimilarity((SimpleDate)clust.getCentroid(), this.reps.get(i).getDate()) > this.DATE_THRESHOLD){
          clust.getElements().add(this.reps.get(i).getId());

          found = true;
          break;
        }
      }

      if ( !found) {
        Cluster newCluster = new Cluster();
        newCluster.setName("Cluster " + (i + 1));
        newCluster.setCentroid(this.reps.get(i).getDate());
        newCluster.getElements().add(this.reps.get(i).getId());

        clusters.add(newCluster);
      }
    }

    return clusters;
  }

  /**
   * Build Hour clusters
   * @return
   */
  public List<Cluster> buildHourClusters(){
    this.loadIndex();


    List<Cluster> clusters = new ArrayList<>();

    Cluster firstCluster = new Cluster();
    firstCluster.setName("Cluster 1");
    firstCluster.setCentroid(this.reps.get(0).getHour());

    firstCluster.getElements().add(this.reps.get(0).getId());

    clusters.add(firstCluster);

    for (int i=1; i < this.reps.size(); i++) {
      if ( this.reps.get(i).getHour() == null)
        continue;

      boolean found = false;

      for ( Cluster clust : clusters){
        if ( tools.hourSimilarity((SimpleHour)clust.getCentroid(), this.reps.get(i).getHour()) > this.HOUR_THRESHOLD){
          clust.getElements().add(this.reps.get(i).getId());

          found = true;
          break;
        }
      }

      if ( !found) {
        Cluster newCluster = new Cluster();
        newCluster.setName("Cluster " + (i + 1));
        newCluster.setCentroid(this.reps.get(i).getHour());
        newCluster.getElements().add(this.reps.get(i).getId());

        clusters.add(newCluster);
      }
    }

    return clusters;
  }

  public void buildClusters() throws ParseException {
    this.loadIndex();

    this.venue_clusters = this.buildVenueClusters();

    this.date_clusters = this.buildDateClusters();

    this.hour_clusters = this.buildHourClusters();
    /*
      index cluster
    */

    this.es.indexRepClusters(this.venue_clusters, this.date_clusters, this.hour_clusters);
  }


  /**
   * This function computes the module of a vector given as parameter
   * @param v
   * @return
   */
  public double module(double[] v){
    double square_sum = 0.0;

    for ( double d : v) {
      square_sum += Math.pow(d, 2);
    }
    return  Math.sqrt(square_sum);
  }

  public double scalar_product(double[] v1, double[] v2){
    double scalar_product = 0.0;
    for (int i =0; i< v1.length; i++)
      scalar_product += (v1[i]*v2[i]);

    return scalar_product;
  }

  /**
   * This function computes the cosine between two vectors of the space
   * @param v1
   * @param v2
   * @return
   */
  private double cosine(double[] v1, double[] v2){
    if ( v1 == null || v2 == null) {
      System.out.println("Attention un des deux vecteurs est nul");
      return 0.0;
    }

    if (v1.length != v2.length){
      System.out.println("Les deux vecteurs doivent avoir la même taille");
      return 0.0;
    }

    double scalar_product = 0.0;
    for (int i =0; i< v1.length; i++)
      scalar_product += (v1[i]*v2[i]);

    return scalar_product/(this.module(v1)*this.module(v2));
  }

  private double similarityWithClustering(String id1, String id2) throws ParserConfigurationException, SAXException, IOException, ParseException {
    double[] v1 = this.eventVector(id1);

    double[] v2 = this.eventVector(id2);

    return this.cosine(v1, v2);
  }

  /**
   * Compute the IDF of a cluster
   * @param c
   * @return
   */
  private double cluster_inverse_frequency(Cluster c){
    return Math.log((double)(this.nb_EN/c.getElements().size()));
  }

  /**
   * Compute the spatial similarity of two representation based on their shape
   * @param e1
   * @param e2
   * @return
   */
  public double venueSimilarity(Object e1, Object e2){

    if ( e1 == null || e2 == null )
      return 0.0;

    if ( e1 instanceof  GeoPoint && e2 instanceof GeoPoint)
      return tools.geoPointSimilarity((GeoPoint) e1, (GeoPoint) e2);

    if ( e1 instanceof  List && e2 instanceof List)
      return tools.geoPolygonSimilarity((List)e1, (List)e2);

    if ( e1 instanceof  GeoPoint && e2 instanceof List)
      return tools.pointPolygonSimilarity((GeoPoint)e1, (List)e2);

    if ( e1 instanceof  List && e2 instanceof GeoPoint)
      return tools.pointPolygonSimilarity((GeoPoint)e2, (List)e1);

    return -1.0;
  }

  /**
   * Build a cluster vector
   * @param id
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public double[] eventVector(String id) throws IOException, SAXException, ParserConfigurationException, ParseException {

    double[] vect = new double[this.venue_clusters.size() + this.date_clusters.size() + this.hour_clusters.size()];

    RepEntity e =  tools.getRepresentation(id);

    int i = 0;


    for( Cluster c : venue_clusters) {
      // Get the clusters
      if (e.getVenue() != null) {
        double coef = this.venueSimilarity(new GeoPoint((double)((Map)c.getCentroid()).get("lat"), (double)((Map)c.getCentroid()).get("lon")), e.getVenue()); //> this.VENUE_THRESHOLD ? 1 : 0;
        vect[i] = this.cluster_inverse_frequency(c)*coef;

      } else {
        vect[i] = 0.0;
      }

      i++;
    }

    for ( Cluster c : date_clusters) {
      if ( e.getDate() != null ) {

        SimpleDate center = new SimpleDate((String)c.getCentroid());

        double coef =  tools.dateSimilarity(center, e.getDate()); // > this.DATE_THRESHOLD ? 1 : 0;

        vect[i] = this.cluster_inverse_frequency(c)*coef;

      }
      else {
        vect[i] = 0.0;
      }

      i++;
    }

    for (Cluster c : hour_clusters) {
      if ( e.getHour() != null ) {
        double coef = tools.hourSimilarity(new SimpleHour((Map)c.getCentroid()), e.getHour()) > this.HOUR_THRESHOLD ? 1 : 0;
        vect[i] = this.cluster_inverse_frequency(c)*coef;
      }
      else {
        vect[i] = 0.0;
      }

      i++;
    }

    return  vect;
  }

  /**
   * Compute the similarity of a set of event couple
   * @param file
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  public void similarityOfSet(String file) throws IOException, ParserConfigurationException, SAXException, ParseException {
    this.loadIndex();

    // Load data from ES
    this.loadRepClusters();

    File inputFile = new File(file);
    // Read existing file
    CSVReader reader = new CSVReader(new FileReader(inputFile), '\t');
    List<String[]> csvBody = reader.readAll();

    int i = 0;
    for ( String[] line : csvBody){
      if (i == 0) {
        i++;
        continue;
      }
      line[9] = "" + this.similarityWithClustering(line[1], line[2]);
      System.out.print(tools.printArray(line));
    }
  }

  public static void main(String[] args) throws Exception {
    Clustering clut = new Clustering("localhost", 9300,  "representation_similarity", "representation");
    // clut.buildClusters();
    clut.similarityOfSet("resources/dataset.csv");
    // System.out.println(clut.similarityWithClustering("6", "7"));
  }

}
