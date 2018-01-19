package com.cogniteev.cognisearch.event.similarity;

import au.com.bytecode.opencsv.CSVReader;
import com.cogniteev.cognisearch.event.model.EventEntity;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by armel on 07/06/17.
 * based on Tpeng and Matthieu Labas regression model
 * cf : https://github.com/tpeng/logistic-regression/blob/master/src/Logistic.java
 */
public class LogisticRegression {

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

  public LogisticRegression(String es_host, int es_port, String index, String type) {
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
    double logit = .0;

    for (int i=0; i<this.weights.length;i++)  {
      logit += this.weights[i] * x[i];
    }
    return logit;
  }


  /**
   *
   * @param e1
   * @param e2
   * @return
   */
  public double titleSimilarity(EventEntity e1, EventEntity e2){
    if ( e1.getTitle() == null || e2.getTitle() == null)
      return this.UNKNOW;
    return tools.softTFIDFscore(e1.getTitle(), e2.getTitle());
  }


  public double categorySimilarity(EventEntity e1, EventEntity e2) throws IOException, SAXException, ParserConfigurationException {
    if ( e1.getCategories() == null || e2.getCategories() == null)
      return this.UNKNOW;

    return tools.ensembleSimilarity(e1.getCategories(), e2.getCategories(), "Concept");
  }

  public double performerSimilarity(EventEntity e1, EventEntity e2) throws IOException, SAXException, ParserConfigurationException {
    if ( e1.getPerformers() == null || e2.getPerformers() == null)
      return this.UNKNOW;

    return tools.ensembleSimilarity(e1.getPerformers(), e2.getPerformers(), "ShortText");
  }


  /**
   * Build the traning matrix
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

    List<EventEntity> entries = null;
    int i = 0;
    for ( String[] line : csvBody){
      if (i == 0) {
        i++;
        continue;
      };
      entries = tools.getEventsFromES(line[0], line[1]);
      x[i-1][0] = this.titleSimilarity(entries.get(0), entries.get(1));
      x[i-1][1] = this.categorySimilarity(entries.get(0), entries.get(1));
      x[i-1][2] = this.performerSimilarity(entries.get(0), entries.get(1));
      x[i-1][3] = Double.parseDouble(line[3]);

      y[i-1] = Double.parseDouble(line[2].replace(",", "."));
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
   * Give two element a
   * @param id1
   * @param id2
   * @param cat
   * @return
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  public double similarityLinearRegression(String id1, String id2, double cat) throws ParserConfigurationException, SAXException, IOException {
    // Build the model
    List<Instance> instances =  new ArrayList<>();
    instances = this.readDataSet("resources/training.csv");
    this.train(instances);

    List<EventEntity> entries = tools.getEventsFromES(id1, id2);

    double[] vect = {this.titleSimilarity(entries.get(0), entries.get(1)),
                      this.categorySimilarity(entries.get(0), entries.get(1)),
                      this.performerSimilarity(entries.get(0), entries.get(1)),
                      cat
    };

    return this.sigmoid(this.classify(vect));
  }

  /**
   *
   * @param testFile
   * @param trainingFile
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  public void similarityOfSet(String testFile, String trainingFile) throws IOException, ParserConfigurationException, SAXException {
    // Build the model
    List<Instance> instances =  new ArrayList<>();
    instances = this.readDataSet(trainingFile);
    this.train(instances);

    File inputFile = new File(testFile);
    // Read existing file
    CSVReader reader = new CSVReader(new FileReader(inputFile), '\t');
    List<String[]> csvBody = reader.readAll();

    int i = 0;
    for ( String[] line : csvBody){
      if (i == 0) {
        i++;
        continue;
      }
      line[7] = "" + this.similarityLinearRegression(line[0], line[1], Double.parseDouble(line[3]));
      System.out.print(tools.printArray(line));

    }
  }



  public static void main(String[] args) throws Exception {
    LogisticRegression logistic = new LogisticRegression("localhost", 9300, "events_similarity", "event");

    String testFile = "resources/testset.csv";
    String trainingFile = "resources/training.csv";

    System.out.println(logistic.similarityLinearRegression("102", "168", 2));
    // logistic.similarityOfSet(testFile, trainingFile);

  }
}
