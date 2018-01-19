package com.cogniteev.cognisearch.event.similarity;

import au.com.bytecode.opencsv.CSVReader;
import com.cogniteev.cognisearch.event.model.EventEntity;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by armel on 06/06/17.
 */
public class CombMNZ {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CombMNZ.class);

  private Utils tools;

  public CombMNZ(String es_host, int es_port, String index, String type){
    tools =  new Utils(es_host, es_port, index, type);
  }

  /**
   * Compute the CombMNZ score between two Event entities given by their ID
   * @param id1
   * @param id2
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public double similarityWithCombMNZ(String id1, String id2) throws IOException, SAXException, ParserConfigurationException {
    List<EventEntity> entries = tools.getEventsFromES(id1, id2);

    if ( entries == null || entries.size() < 2)
      return -1.0;

    int nb_not_null = 0;

    double res = 0.0;

    if ( entries.get(0).getTitle() == null || entries.get(1).getTitle() == null)
      return -1.0;

    nb_not_null ++;

    res += tools.softTFIDFscore(entries.get(0).getTitle(), entries.get(1).getTitle());



    if ( entries.get(0).getCategories() != null && entries.get(1).getCategories() != null) {
      if ( tools.ensembleSimilarity(entries.get(0).getCategories(), entries.get(1).getCategories(), "Concept") < 0)
        return -1.0;

      nb_not_null ++;

      res += tools.ensembleSimilarity(entries.get(0).getCategories(), entries.get(1).getCategories(), "Concept");
    }



    if ( entries.get(0).getPerformers() != null && entries.get(1).getPerformers() != null) {
      if ( tools.ensembleSimilarity(entries.get(0).getPerformers(), entries.get(1).getPerformers(), "ShortText") < 0)
        return -1.0;

      nb_not_null ++;

      res += tools.ensembleSimilarity(entries.get(0).getPerformers(), entries.get(1).getPerformers(), "ShortText");
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
    // ArrayList<String[]> lines = tools.parseCsv(testfile);
    File inputFile = new File(testfile);
    // Read existing file
    CSVReader reader = new CSVReader(new FileReader(inputFile), ',');
    List<String[]> csvBody = reader.readAll();

    int i = 0;
    for ( String[] line : csvBody){
      if (i == 0) {
        i++;
        continue;
      }
      line[4] = "" + this.similarityWithCombMNZ(line[0], line[1]);
      System.out.print(tools.printArray(line));
    }
  }


  public static void main(String[] args) throws Exception {
    CombMNZ comb = new CombMNZ("localhost", 9300, "events_similarity", "event");
      System.out.println(comb.similarityWithCombMNZ("166", "167"));
    // comb.similarityOfSet("resources/testset.csv");
  }
}
