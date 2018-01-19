package com.cogniteev.cognisearch.event.repSimilarity;

import com.cogniteev.cognisearch.event.similarity.Utils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.LoggerFactory;

/**
 * Created by armel on 26/07/17.
 */
public class Worker {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Worker.class);

  private Client client;
  private String index;
  private String type;
  private Utils tools;

  public Worker(String es_host, int es_port, String index, String type){
    client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(es_host, es_port));
    this.index = index;
    this.type = type;
    this.tools = new Utils();
  }


  public static void main(String[] args) throws Exception {
    Worker worker = new Worker("localhost", 9300, "representation_similarity", "representation");
    // System.out.println(worker.getRepsFromES("98", "143"));
  }
}
