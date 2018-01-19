package com.cogniteev.cognisearch.event.similarity;

import au.com.bytecode.opencsv.CSVReader;
import com.cogniteev.cognisearch.event.model.EventEntity;
import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Created by armel on 06/06/17.
 */
public class LinearRegression {
  private Utils tools;
  private double[][] x = null;
  private double[]   y = null;
  private OLSMultipleLinearRegression regression = null;
  private final double UNKNOW = 0.5;
  private final int nbFeat = 4;

  public LinearRegression(String es_host, int es_port, String index, String type){
    tools =  new Utils(es_host, es_port, index, type);
    regression = new OLSMultipleLinearRegression();
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

  public String printMatrix(double[][] mat){
    String res = "";
    for( int i=0; i < mat.length; i++){
      for ( int j=0; j<this.nbFeat; j++)
        res += mat[i][j] + "\t";
      res += "\n";
    }
    return res;
  }



  public void removeDuplicates(double[][] data, double[] pred){
    HashSet<String> hashSet = new HashSet<String>();
    List<double[]> notDuplicate = new ArrayList<>();
    List<Double> notDuplY = new  ArrayList<>();

    int pos = 0;
    for( double[] row : data){
      if(!hashSet.contains(Arrays.toString(row))){
        notDuplicate.add(row);
        notDuplY.add(pred[pos]);
        hashSet.add(Arrays.toString(row));
      }
      pos ++;
    }

    // build the returned matrix
    double[][] x = new double[notDuplicate.size()][this.nbFeat];

    int i= 0;
    for( double[] r : notDuplicate) {
      x[i] = r;
      i++;
    }

    // Build Y
    int j = 0;
    double[] y = new double[notDuplicate.size()];
    for ( double elt : notDuplY){
      y[j] = elt;
      j++;
    }


    this.x = x;
    this.y = y;
  }

  /**
   *
   * @param trainingFile
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
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

    // this.removeDuplicates(x, y);

  }

  public void buildModel(String trainingFile) throws ParserConfigurationException, SAXException, IOException {
    this.buildMatrix(trainingFile);
    this.regression.newSampleData(this.y, this.x);
    for (int i=0; i< regression.estimateRegressionParameters().length; i++)
      System.out.println(this.regression.estimateRegressionParameters()[i]);

  }

  public static void main(String[] args) throws Exception {
    LinearRegression worker = new LinearRegression("localhost", 9300, "events_similarity", "event");
    worker.buildModel("resources/testset.csv");
    /*OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
    double[] y = new double[]{11.0, 12.0, 13.0, 14.0, 15.0, 16.0};
    double[][] x = new double[6][];
    x[0] = new double[]{0, 0, 0, 0, 0};
    x[1] = new double[]{2.0, 0, 0, 0, 0};
    x[2] = new double[]{0, 3.0, 0, 0, 0};
    x[3] = new double[]{0, 0, 4.0, 0, 0};
    x[4] = new double[]{0, 0, 0, 5.0, 0};
    x[5] = new double[]{0, 0, 0, 0, 6.0};
    regression.newSampleData(y, x);

    for (int i=0; i< regression.estimateRegressionParameters().length; i++)
      System.out.println(regression.estimateRegressionParameters()[i]);
    */
  }
}
