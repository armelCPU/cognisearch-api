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
public class Empirique {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Empirique.class);

  private Utils tools;
  private final double TITLE_WEIGHT = 0.5;
  private final double CATEGORY_WEIGHT = 0.3;
  private final double PERFORMER_WEIGHT = 0.2;

  public Empirique(String es_host, int es_port, String index, String type){
    tools =  new Utils(es_host, es_port, index, type);
  }


  /**
   * Compute the similarity between two elements using empiric weight for linear regression
   * @param id1
   * @param id2
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public double similarityWithEmpiricWeight(String id1, String id2) throws IOException, SAXException, ParserConfigurationException {
    List<EventEntity> entries = tools.getEventsFromES(id1, id2);

    double title_weight = this.TITLE_WEIGHT;
    double cat_weight = this.CATEGORY_WEIGHT;
    double perf_weght = this.PERFORMER_WEIGHT;


    if ( entries == null || entries.size() < 2)
      return -1.0;

    if ( entries.get(0).getTitle() == null || entries.get(1).getTitle() == null)
      return -1.0;

    // Case where one category and/or one performer is empty
    if ( (entries.get(0).getCategories() == null || entries.get(1).getCategories() == null) && (entries.get(0).getPerformers() == null || entries.get(1).getPerformers() == null)) {
      return  tools.softTFIDFscore(entries.get(0).getTitle(), entries.get(1).getTitle());
    }

    // Case where categories are not null and performers are null
    if ( (entries.get(0).getCategories() != null && entries.get(1).getCategories() != null) && (entries.get(0).getPerformers() == null || entries.get(1).getPerformers() == null) ){
      title_weight = this.TITLE_WEIGHT + (this.PERFORMER_WEIGHT/2);
      cat_weight = this.CATEGORY_WEIGHT + (this.PERFORMER_WEIGHT/2);

      return title_weight*tools.softTFIDFscore(entries.get(0).getTitle(), entries.get(1).getTitle())
          + cat_weight*tools.ensembleSimilarity(entries.get(0).getCategories(), entries.get(1).getCategories(), "Concept");
    }

    // Case where performers are not null and categories are not
    if ( (entries.get(0).getPerformers() != null || entries.get(1).getPerformers() != null) && (entries.get(0).getCategories() == null || entries.get(1).getCategories() == null)) {
      title_weight = this.TITLE_WEIGHT + (this.CATEGORY_WEIGHT/2);
      perf_weght = this.PERFORMER_WEIGHT + (this.CATEGORY_WEIGHT/2);

      return title_weight*tools.softTFIDFscore(entries.get(0).getTitle(), entries.get(1).getTitle())
          + perf_weght*tools.ensembleSimilarity(entries.get(0).getPerformers(), entries.get(1).getPerformers(), "ShortText");
    }

    return this.TITLE_WEIGHT*tools.softTFIDFscore(entries.get(0).getTitle(), entries.get(1).getTitle()) +
        this.CATEGORY_WEIGHT*tools.ensembleSimilarity(entries.get(0).getCategories(), entries.get(1).getCategories(), "Concept") +
        this.PERFORMER_WEIGHT*tools.ensembleSimilarity(entries.get(0).getPerformers(), entries.get(1).getPerformers(), "ShortText");
  }


  /**
   * Compute the similarity of a set of event couple
   * @param file
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  public void similarityOfSet(String file) throws IOException, ParserConfigurationException, SAXException {
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
      line[6] = "" + this.similarityWithEmpiricWeight(line[0], line[1]);
      System.out.print(tools.printArray(line));

    }
  }

  public static void main(String[] args) throws Exception {
    Empirique comb = new Empirique("localhost", 9300, "events_similarity", "event");
    System.out.println(comb.similarityWithEmpiricWeight("169", "91"));
    //comb.similarityOfSet("resources/testset.csv");
  }
}
