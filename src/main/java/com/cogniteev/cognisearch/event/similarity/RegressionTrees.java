package com.cogniteev.cognisearch.event.similarity;

import au.com.bytecode.opencsv.CSVReader;
import com.cogniteev.cognisearch.event.model.EventEntity;
import org.xml.sax.SAXException;
import smile.regression.RegressionTree;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by armel on 07/06/17.
 */
public class RegressionTrees {

  private Utils tools;
  private double[][] x = null;
  private double[]   y = null;
  private final double UNKNOW = -0.5;
  private final int nbFeat = 4;
  private final int MAX_NODES = 10;
  private RegressionTree regressionModel = null;

  public RegressionTrees(String es_host, int es_port, String index, String type) {
    tools = new Utils(es_host, es_port, index, type);
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


  public double categorySimilarity(EventEntity e1, EventEntity e2) throws IOException, SAXException, ParserConfigurationException
  {
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
   *
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

    if ( this.x.length > 0 && this.y.length >0 )
      this.regressionModel = new RegressionTree(this.x, this.y, this.MAX_NODES);
  }


  /**
   *
   * @param id1
   * @param id2
   * @param cat
   * @return
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  public double similarityNonLinearRegression(String id1, String id2, double cat)throws ParserConfigurationException, SAXException, IOException {
    List<EventEntity> entries = tools.getEventsFromES(id1, id2);

    double[] vect = {this.titleSimilarity(entries.get(0), entries.get(1)),
        this.categorySimilarity(entries.get(0), entries.get(1)),
        this.performerSimilarity(entries.get(0), entries.get(1)),
        cat
    };

    return this.regressionModel.predict(vect);
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
    this.buildMatrix(trainingFile);

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
      line[8] = "" + this.similarityNonLinearRegression(line[0], line[1], Double.parseDouble(line[3]));
      System.out.print(tools.printArray(line));

    }
  }

  public static void main(String[] args) throws Exception {
    RegressionTrees nlReg = new RegressionTrees("localhost", 9300, "events_similarity", "event");

    String trainingFile = "resources/training.csv";

    nlReg.buildMatrix(trainingFile);

    System.out.println(nlReg.similarityNonLinearRegression("166", "167", 3));
    /*
    String testFile = "resources/testset.csv";
    String trainingFile = "resources/training.csv";

    nlReg.similarityOfSet(testFile, trainingFile);
    */
  }
}
