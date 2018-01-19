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
 * Created by armel on 26/07/17.
 */
public class CombMNZ {
  private Utils tools;

  public CombMNZ(String es_host, int es_port, String index, String type){
    tools =  new Utils(es_host, es_port, index, type);
  }

  /**
   * Compute the CombMNZ score between two Representations given by their ID
   * @param id1
   * @param id2
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public double similarityWithCombMNZ(String id1, String id2) throws IOException, SAXException, ParserConfigurationException {
    List<RepEntity> entries = tools.getRepsFromES(id1, id2);
    double res = 0.0;

    if ( entries == null || entries.size() < 2)
      return -1.0;

    int nb_not_null = 0;

    if ( entries.get(0).getVenue() == null || entries.get(1).getVenue() == null)
      return -1.0;

    nb_not_null ++;

    if ( entries.get(0).getVenue() instanceof GeoPoint && entries.get(1).getVenue() instanceof GeoPoint)
      res += tools.geoPointSimilarity((GeoPoint)entries.get(0).getVenue(), (GeoPoint)entries.get(1).getVenue());

    if ( entries.get(0).getVenue() instanceof List && entries.get(1).getVenue() instanceof List){
      res += tools.geoPolygonSimilarity((List) entries.get(0).getVenue(), (List) entries.get(1).getVenue());
    }

    if (entries.get(0).getVenue() instanceof GeoPoint && entries.get(1).getVenue() instanceof List) {
      res += tools.pointPolygonSimilarity((GeoPoint) entries.get(0).getVenue(), (List) entries.get(1).getVenue());
    }

    if( entries.get(0).getVenue() instanceof List && entries.get(1).getVenue() instanceof GeoPoint){
      res += tools.pointPolygonSimilarity((GeoPoint) entries.get(1).getVenue(), (List) entries.get(0).getVenue());
    }



    if ( entries.get(0).getDate() != null && entries.get(1).getDate() != null){
      if ( tools.dateSimilarity(entries.get(0).getDate(), entries.get(1).getDate()) < 0)
        return -1.0;

      nb_not_null ++;

      res+= tools.dateSimilarity(entries.get(0).getDate(), entries.get(1).getDate());
    }


    if ( entries.get(0).getHour() != null && entries.get(1).getHour() != null){
      if ( tools.hourSimilarity(entries.get(0).getHour(), entries.get(1).getHour()) < 0)
        return -1.0;

      nb_not_null ++;

      res+= tools.hourSimilarity(entries.get(0).getHour(), entries.get(1).getHour());
    }

    return res*nb_not_null;
  }



  /**
   * Compute the similarity of a set of event couple
   * @param testfile
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
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
      line[5] = "" + this.similarityWithCombMNZ(line[1], line[2]);
      System.out.print(tools.printArray(line));
    }
  }

  public static void main(String[] args) throws Exception {
    CombMNZ comb = new CombMNZ("localhost", 9300, "representation_similarity", "representation");
    //System.out.println(comb.similarityWithCombMNZ("125", "128"));
    comb.similarityOfSet("resources/dataset.csv");
  }
}
