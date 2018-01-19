package com.cogniteev.cognisearch.event.similarity;

import com.cogniteev.cognisearch.event.model.EventEntity;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by armel on 25/05/17.
 */
public class Worker {

  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Worker.class);

  private Client client;
  private String index;
  private String type;
  private Utils tools;

  private final double TITLE_WEIGHT = 0.5;
  private final double CATEGORY_WEIGHT = 0.3;
  private final double PERFORMER_WEIGHT = 0.2;

  public Worker(String es_host, int es_port, String index, String type){
    client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(es_host, es_port));
    this.index = index;
    this.type = type;
    this.tools = new Utils();
  }

  /**
   * Get two EventEntities from ES by using their IDs
   * @param id1
   * @param id2
   * @return
   */
  public List<EventEntity> getEventsFromES(String id1, String id2) {
    SearchResponse resp = client.prepareSearch(index).setTypes(type).setQuery(QueryBuilders.boolQuery().should(QueryBuilders.termQuery("_id", id1)).should(QueryBuilders.termQuery("_id", id2))).execute().actionGet();
    SearchHit[] hits = resp.getHits().getHits();

    if (hits == null || hits.length < 2)
      return null;

    // Else we have at least on result
    List<EventEntity> res = new ArrayList<EventEntity>();

    res.add(new EventEntity(hits[0].getSource(), hits[0].getId()));
    res.add(new EventEntity(hits[1].getSource(), hits[0].getId()));

    return res;
  }


  /**
   * This function computes the similarity between two EventEntities referenced by their IDs
   * -1.0 is returned if there is an error
   * @param id1
   * @param id2
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public double empiricSimilarity(String id1, String id2) throws IOException, SAXException, ParserConfigurationException {
    List<EventEntity> entries = this.getEventsFromES(id1, id2);

    if ( entries == null || entries.size() < 2)
      return -1.0;


    double res = 0.0;

    if ( tools.softTFIDFscore(entries.get(0).getTitle(), entries.get(1).getTitle()) < 0)
      return -1.0;

    res += this.TITLE_WEIGHT * tools.softTFIDFscore(entries.get(0).getTitle(), entries.get(1).getTitle());
    System.out.println("Score titre : " + res/this.TITLE_WEIGHT);

    if ( entries.get(0).getCategories() != null && entries.get(1).getCategories() != null) {
      if ( tools.ensembleSimilarity(entries.get(0).getCategories(), entries.get(1).getCategories(), "Concept") < 0)
        return -1.0;

      res += this.CATEGORY_WEIGHT * tools.ensembleSimilarity(entries.get(0).getCategories(), entries.get(1).getCategories(), "Concept");
      System.out.println("Score category : " + tools.ensembleSimilarity(entries.get(0).getCategories(), entries.get(1).getCategories(), "Concept"));;
    }


    if ( entries.get(0).getPerformers() != null && entries.get(1).getPerformers() != null) {
      if ( tools.ensembleSimilarity(entries.get(0).getPerformers(), entries.get(1).getPerformers(), "ShortText") < 0)
        return -1.0;

      res += this.PERFORMER_WEIGHT * tools.ensembleSimilarity(entries.get(0).getPerformers(), entries.get(1).getPerformers(), "ShortText");
      System.out.println("Score performers : " + tools.ensembleSimilarity(entries.get(0).getPerformers(), entries.get(1).getPerformers(), "ShortText"));
    }


    return res;
  }


  public static void main(String[] args) throws Exception {
    Worker worker = new Worker("localhost", 9300, "events_similarity", "event");
    System.out.println(worker.empiricSimilarity("98", "166"));
  }
}
