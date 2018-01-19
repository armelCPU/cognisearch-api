package com.cogniteev.cognisearch.event.repSimilarity;

import au.com.bytecode.opencsv.CSVReader;
import com.cogniteev.cognisearch.event.model.RepEntity;
import com.cogniteev.cognisearch.event.similarity.Utils;
import org.elasticsearch.common.geo.GeoPoint;
import org.xml.sax.SAXException;
import smile.regression.RegressionTree;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by armel on 27/07/17.
 */
public class RegressionTrees {
  private Utils tools;
  private double[][] x = null;
  private double[]   y = null;
  private final double UNKNOW = -0.5;
  private final int nbFeat = 4;
  private final int MAX_NODES = 6;
  private RegressionTree regressionModel = null;

  public RegressionTrees(String es_host, int es_port, String index, String type) {
    tools = new Utils(es_host, es_port, index, type);
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
    else {
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

    // Train Model

    if ( this.x.length > 0 && this.y.length > 0 )
      this.regressionModel = new RegressionTree(this.x, this.y, this.MAX_NODES);
  }

  /**
   *
   * @param id1
   * @param id2
   * @param cat
   * @return
   */
  public double similarityWithRegressionTrees(String id1, String id2, double cat){
    List<RepEntity> entries = tools.getRepsFromES(id1, id2);

    double[] vect = {this.venueSimilarity(entries), this.dateSimilarity(entries), this.hourSimilarity(entries), cat};

    return this.regressionModel.predict(vect);
  }

  /**
   * Compute the similarity of a set of representation couple
   * @param testfile
   * @throws javax.xml.parsers.ParserConfigurationException
   * @throws org.xml.sax.SAXException
   * @throws java.io.IOException
   */
  public void similarityOfSet(String testfile, String trainingFile) throws ParserConfigurationException, SAXException, IOException {

    this.buildMatrix(trainingFile);

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
      line[8] = "" + this.similarityWithRegressionTrees(line[1], line[2], Double.parseDouble(line[4]));
      System.out.print(tools.printArray(line));
    }
  }

  public static void main(String[] args) throws Exception {
    RegressionTrees nlReg = new RegressionTrees("localhost", 9300, "representation_similarity", "representation");

    nlReg.similarityOfSet("resources/dataset.csv", "resources/rep_training.csv");
  }
}
