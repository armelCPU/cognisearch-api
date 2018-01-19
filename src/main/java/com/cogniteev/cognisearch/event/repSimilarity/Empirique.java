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
import java.util.List;

/**
 * Created by armel on 27/07/17.
 */
public class Empirique {
  private Utils tools;
  /*private final double VENUE_WEIGHT = 0.31;
  private final double DATE_WEIGHT = 0.36;
  private final double HOUR_WEIGHT = 0.33;*/
  private final double VENUE_WEIGHT = 0.5;
  private final double DATE_WEIGHT = 0.5;
  private final double HOUR_WEIGHT = 0.0;

  public Empirique(String es_host, int es_port, String index, String type){
    tools =  new Utils(es_host, es_port, index, type);
  }

  public double similarityWithEmpiricWeight(String id1, String id2){
    double venue_weight = this.VENUE_WEIGHT;
    double date_weight = this.DATE_WEIGHT;
    double hour_weight = this.HOUR_WEIGHT;
    double venue_sim = 0.0;

    List<RepEntity> entries = tools.getRepsFromES(id1, id2);
    double res = 0.0;

    if ( entries == null || entries.size() < 2)
      return -1.0;

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

    // Case where one date and one performer empty  --> Only the venue is necessary for the similarity computing
    if ( (entries.get(0).getDate() == null || entries.get(1).getDate() == null) && (entries.get(0).getHour() == null || entries.get(1).getHour() == null)) {
      return venue_sim;
    }

    // Case where dates are not null but performers are !
    if ( (entries.get(0).getDate() != null && entries.get(1).getDate() != null) && (entries.get(0).getHour() == null || entries.get(1).getHour() == null)){
      venue_weight = this.VENUE_WEIGHT + (this.HOUR_WEIGHT/2);
      date_weight = this.DATE_WEIGHT + (this.HOUR_WEIGHT/2);

      return venue_weight*venue_sim + date_weight*tools.dateSimilarity(entries.get(0).getDate(), entries.get(1).getDate());
    }

    // case where hours are not null and categories are
    if ( (entries.get(0).getHour() != null && entries.get(1).getHour() != null) && (entries.get(0).getDate() == null || entries.get(1).getDate() == null)) {
      venue_weight = this.VENUE_WEIGHT + (this.DATE_WEIGHT/2);
      hour_weight = this.HOUR_WEIGHT + (this.DATE_WEIGHT/2);

      return  venue_weight*venue_sim + hour_weight*tools.hourSimilarity(entries.get(0).getHour(), entries.get(1).getHour());
    }

    // All properties are registered
    return this.VENUE_WEIGHT*venue_sim + this.DATE_WEIGHT*tools.dateSimilarity(entries.get(0).getDate(), entries.get(1).getDate()) +
        + this.HOUR_WEIGHT*tools.hourSimilarity(entries.get(0).getHour(), entries.get(1).getHour());
  }


  /**
   * Compute the similarity of a set of event couple
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
      line[6] = "" + this.similarityWithEmpiricWeight(line[1], line[2]);
      System.out.print(tools.printArray(line));
    }
  }

  public static void main(String[] args) throws Exception {
    Empirique emp = new Empirique("localhost", 9300, "representation_similarity", "representation");
    // System.out.println(emp.similarityWithEmpiricWeight("1", "2"));
    emp.similarityOfSet("resources/dataset.csv");
  }
}
