package com.cogniteev.cognisearch.event.repSimilarity;

import au.com.bytecode.opencsv.CSVReader;
import com.cogniteev.cognisearch.event.model.RepEntity;
import com.cogniteev.cognisearch.event.similarity.Utils;
import org.elasticsearch.common.geo.GeoPoint;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by armel on 27/07/17.
 */
public class LogisticalRegression {
  private Utils tools;
  private double[][] x = null;
  private double[]   y = null;
  private final double UNKNOW = -0.5;
  private final int nbFeat = 4;

  /** the learning rate */
  private double rate;

  /** the weight to learn */
  private double[] weights;

  /** the number of iterations */
  private int ITERATIONS = 3000;

  public LogisticalRegression(String es_host, int es_port, String index, String type) {
    this.rate = 0.001;
    weights = new double[this.nbFeat];
    tools =  new Utils(es_host, es_port, index, type);
  }

  /**
   * Sigmoid function for generalizing the linear model
   * @param z
   * @return
   */
  private static double sigmoid(double z) {
    return  Math.exp(-z)/ (1.0 + Math.exp(-z));
  }

  /**
   * A simple class for mapping each individual and its label
   */
  public static class Instance {
    public double label;
    public double[] x;

    public Instance(double label, double[] x) {
      this.label = label;
      this.x = x;
    }
  }

  /**
   * This function helps to train the model a evaluating the weight parameters
   * @param instances
   */
  public void train(List<Instance> instances) {
    for (int n=0; n < this.ITERATIONS; n++) {
      double likewood = 0.0;
      for (int i=0; i<instances.size(); i++) {
        double[] x = instances.get(i).x;
        double predicted = classify(x);
        double label = instances.get(i).label;
        for (int j=0; j<weights.length; j++) {
          weights[j] = weights[j] + rate * (label - predicted) * x[j];
        }
        likewood += label * Math.log(classify(x)) + (1-label) * Math.log(1- classify(x));
      }
    }
  }
  /**
   * Given a input vector for features, this function compute the predicted score
   * @param x
   * @return
   */
  private double classify(double[] x) {
    double score = .0;

    for (int i=0; i<this.weights.length;i++)  {
      score += this.weights[i] * x[i];
    }
    return score;
  }

  /**
   * Compute the similarity between two venues
   * @param entries
   * @return
   */
 public double venueSimilarity(List<RepEntity> entries){
   double venue_sim = 0.0;

   if ( entries.get(0).getVenue() == null || entries.get(1).getVenue() == null)
     return 0.0;

   if ( entries.get(0).getVenue() instanceof GeoPoint && entries.get(1).getVenue() instanceof GeoPoint)
     venue_sim = tools.geoPointSimilarity((GeoPoint)entries.get(0).getVenue(), (GeoPoint)entries.get(1).getVenue());

   if ( entries.get(0).getVenue() instanceof List && entries.get(1).getVenue() instanceof List){
     venue_sim = tools.geoPolygonSimilarity((List) entries.get(0).getVenue(), (List) entries.get(1).getVenue());
   }

   if (entries.get(0).getVenue() instanceof GeoPoint && entries.get(1).getVenue() instanceof List) {
     venue_sim = tools.pointPolygonSimilarity((GeoPoint) entries.get(0).getVenue(), (List) entries.get(1).getVenue());
   }

   if( entries.get(0).getVenue() instanceof List && entries.get(1).getVenue() instanceof GeoPoint){
     venue_sim = tools.pointPolygonSimilarity((GeoPoint) entries.get(1).getVenue(), (List) entries.get(0).getVenue());
   }


   return venue_sim;
 }

  /**
   * Calculate the similarity between two dates
   * Return UNKNOW if one of them is null
   * @param entries
   * @return
   */
  public double dateSimilarity(List<RepEntity> entries){
    if ( entries.get(0).getDate() == null || entries.get(1).getDate() == null)
      return this.UNKNOW;

    return tools.dateSimilarity(entries.get(0).getDate(), entries.get(1).getDate());
  }

  /**
   * Calculate the similarity between two hours
   * Return UNKNOW if one is null
   * @param entries
   * @return
   */
  public double hourSimilarity(List<RepEntity> entries){
    if ( entries.get(0).getHour() == null || entries.get(1).getHour() == null)
      return this.UNKNOW;

    return tools.hourSimilarity(entries.get(0).getHour(), entries.get(1).getHour());
  }

  /**
   * Build the training matrix
   * @param trainingFile
   * @throws java.io.IOException
   * @throws javax.xml.parsers.ParserConfigurationException
   * @throws org.xml.sax.SAXException
   */
  public void buildMatrix(String trainingFile) throws IOException, ParserConfigurationException, SAXException {
    // Reading the file
    File inputFile = new File(trainingFile);

    // Read existing file
    CSVReader reader = new CSVReader(new FileReader(inputFile), '\t');
    List<String[]> csvBody = reader.readAll();

    // Matrix
    double[][] x = new double[csvBody.size()][this.nbFeat];
    double[] y = new double[csvBody.size()];

    double logit_y = 0.0;

    List<RepEntity> entries = null;
    int i = 0;
    for ( String[] line : csvBody){
      if (i == 0) {
        i++;
        continue;
      };
      entries = tools.getRepsFromES(line[1], line[2]);

      x[i-1][0] = this.venueSimilarity(entries);
      x[i-1][1] = this.dateSimilarity(entries);
      x[i-1][2] = this.dateSimilarity(entries);
      x[i-1][3] = Double.parseDouble(line[4]);

      y[i-1] = Double.parseDouble(line[3].replace(",", "."));
      i++;
    }

    this.x = x;
    this.y = y;
  }

  /**
   * This function reads data contained in a csv file and build
   * a list of Instance for training a model
   * @param trainingFile
   * @return
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  public List<Instance> readDataSet(String trainingFile) throws ParserConfigurationException, SAXException, IOException {

    this.buildMatrix(trainingFile);

    List<Instance> dataset = new ArrayList<Instance>();

    int i = 0;
    for ( double[] row : this.x) {
      Instance instance = new Instance(this.y[i], row);
      dataset.add(instance);
      i ++;
    }

    return dataset;
  }

  /**
   * Compute the similarity of two Representation entites
   * @param id1
   * @param id2
   * @return
   */
  public double similarityWithLogisticalRegression(String id1, String id2, double cat) throws IOException, SAXException, ParserConfigurationException {
    List<Instance> instances = this.readDataSet("resources/rep_training.csv");

    this.train(instances);

    List<RepEntity> entries = tools.getRepsFromES(id1, id2);

    double[] vect = {this.venueSimilarity(entries), this.dateSimilarity(entries), this.hourSimilarity(entries), cat};

    return this.classify(vect);

  }

  /**
   * Compute the similarity of a set of representation couple
   * @param testfile
   * @throws javax.xml.parsers.ParserConfigurationException
   * @throws org.xml.sax.SAXException
   * @throws java.io.IOException
   */
  public void similarityOfSet(String testfile) throws ParserConfigurationException, SAXException, IOException {

    File inputFile = new File(testfile);
    // Read existing file
    CSVReader reader = new CSVReader(new FileReader(inputFile), '\t');
    List<String[]> csvBody = reader.readAll();

    int i = 0;
    for ( String[] line : csvBody){
      if (i == 0) {
        i++;
        continue;
      }
      line[7] = "" + this.similarityWithLogisticalRegression(line[1], line[2], Double.parseDouble(line[4]));
      System.out.print(tools.printArray(line));
    }
  }

  public static void main(String[] args) throws Exception {
    LogisticalRegression log = new LogisticalRegression("localhost", 9300, "representation_similarity", "representation");

    log.similarityOfSet("resources/dataset.csv");
  }
}
