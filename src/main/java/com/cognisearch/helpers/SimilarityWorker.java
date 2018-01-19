package com.cognisearch.helpers;

import com.cogniteev.cognisearch.event.model.EventEntity;
import com.cogniteev.cognisearch.event.model.RepEntity;
import com.cogniteev.cognisearch.event.similarity.Utils;
import org.elasticsearch.common.geo.GeoPoint;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

/**
 * Created by armel on 04/01/18.
 */
public class SimilarityWorker {

  private Utils tools;
  private final double VENUE_WEIGHT = 0.75;
  private final double DATE_WEIGHT = 0.25;
  private final double HOUR_WEIGHT = 0.0;
  private final double PERF_CATEGORY_WEIGTH = 0.0;
  private final double NAME_WEIGHT = 0.0;
  private final double CATEGORY_WEIGHT = 0.65;
  private final double PERFORMER_WEIGHT = 0.35;
  private final double EVENT_CATEGORY_WEIGHT = 0.0;

  public SimilarityWorker(){
    tools = new Utils();
  }

  public double eventSimilarity(EventEntity e1, EventEntity e2, double cat) throws IOException, SAXException, ParserConfigurationException {

    if( e1 == null || e2 == null)
      return 0.0;

    double name_sim = 0.0;
    double category_sim = 0.0;
    double performers_sim = 0.0;

    if (e1.getTitle() != null && e2.getTitle() != null)
      name_sim = this.tools.softTFIDFscore(e1.getTitle(), e2.getTitle());

    name_sim = name_sim < 0.0 ? 0.0 : name_sim;

    if ( e1.getCategories() != null && e2.getCategories() != null)
      category_sim = this.tools.ensembleSimilarity(e1.getCategories(), e2.getCategories(), "Concept");

    category_sim = category_sim < 0.0 ? 0.0 : category_sim;

    if ( e1.getPerformers() != null && e2.getPerformers() != null)
      performers_sim = this.tools.ensembleSimilarity(e1.getPerformers(), e2.getPerformers(), "ShortText");

    performers_sim = performers_sim < 0.0 ? 0.0 : performers_sim;

    return this.NAME_WEIGHT*name_sim + this.CATEGORY_WEIGHT*category_sim + this.PERFORMER_WEIGHT*performers_sim + this.EVENT_CATEGORY_WEIGHT*category_sim;
  }
  /**
   * Computing the similarity between two performances
   * @param e1
   * @param e2
   * @param cat
   * @return
   */
  public double representationSimilarity(RepEntity e1, RepEntity e2, double cat){

    if( e1 == null || e2 == null)
      return 0.0;

    double venue_sim = 0.0;
    double date_sim  = 0.0;
    double hour_sim  = 0.0;

    if( e1.getVenue() != null && e2.getVenue() != null ) {
      // We have date and venue

      if (e1.getVenue() instanceof List && e2.getVenue() instanceof List)
        venue_sim = this.tools.geoPolygonSimilarity((List) e1.getVenue(), (List) e2.getVenue());

      if (e1.getVenue() instanceof GeoPoint && e2.getVenue() instanceof GeoPoint)
        venue_sim = this.tools.geoPointSimilarity((GeoPoint) e1.getVenue(), (GeoPoint) e2.getVenue());

      if (e1.getVenue() instanceof GeoPoint && e2.getVenue() instanceof List)
        venue_sim = this.tools.pointPolygonSimilarity((GeoPoint) e1.getVenue(), (List) e2.getVenue());

      if (e2.getVenue() instanceof GeoPoint && e1.getVenue() instanceof List)
        venue_sim = this.tools.pointPolygonSimilarity((GeoPoint) e2.getVenue(), (List) e1.getVenue());
    }

    venue_sim = venue_sim == -1.0 ? 0.0 : venue_sim;

    if( e1.getDate() != null && e2.getDate() != null)
      date_sim = this.tools.dateSimilarity(e1.getDate(), e2.getDate());

    date_sim = date_sim == -1.0 ? 0.0 : date_sim;

    if (e1.getHour() != null && e2.getHour() != null)
      hour_sim = this.tools.hourSimilarity(e1.getHour(), e2.getHour());

    hour_sim = hour_sim == -1.0 ? 0.0 : hour_sim;

    return this.VENUE_WEIGHT*venue_sim + this.DATE_WEIGHT*date_sim + this.HOUR_WEIGHT*hour_sim + this.PERF_CATEGORY_WEIGTH*cat;

    /*
    if( (e1.getVenue() != null && e2.getVenue() != null) && (e1.getDate() != null && e2.getDate() != null) ){
      // We have date and venue

      if ( e1.getVenue() instanceof List && e2.getVenue() instanceof  List)
        venue_sim = this.tools.geoPolygonSimilarity((List)e1.getVenue(), (List)e2.getVenue());

      if ( e1.getVenue() instanceof GeoPoint && e2.getVenue() instanceof  GeoPoint)
        venue_sim = this.tools.geoPointSimilarity((GeoPoint)e1.getVenue(), (GeoPoint)e2.getVenue());

      if ( e1.getVenue() instanceof GeoPoint && e2.getVenue() instanceof List)
        venue_sim = this.tools.pointPolygonSimilarity((GeoPoint)e1.getVenue(), (List)e2.getVenue());

      if ( e2.getVenue() instanceof GeoPoint && e1.getVenue() instanceof List)
        venue_sim = this.tools.pointPolygonSimilarity((GeoPoint)e2.getVenue(), (List)e1.getVenue());

      return venue_sim*this.VENUE_WEIGHT + this.tools.dateSimilarity(e1.getDate(), e2.getDate())*this.DATE_WEIGHT;
    }

    if( (e1.getVenue() != null && e2.getVenue() != null) && (e1.getDate() != null || e2.getDate() != null) ){
      // We have date and venue
      if ( e1.getVenue() instanceof List && e2.getVenue() instanceof  List)
        venue_sim = this.tools.geoPolygonSimilarity((List)e1.getVenue(), (List)e2.getVenue());

      if ( e1.getVenue() instanceof GeoPoint && e2.getVenue() instanceof  GeoPoint)
        venue_sim = this.tools.geoPointSimilarity((GeoPoint)e1.getVenue(), (GeoPoint)e2.getVenue());

      if ( e1.getVenue() instanceof GeoPoint && e2.getVenue() instanceof List)
        venue_sim = this.tools.pointPolygonSimilarity((GeoPoint)e1.getVenue(), (List)e2.getVenue());

      if ( e2.getVenue() instanceof GeoPoint && e1.getVenue() instanceof List)
        venue_sim = this.tools.pointPolygonSimilarity((GeoPoint)e2.getVenue(), (List)e1.getVenue());

      return venue_sim;
    }

    if( (e1.getVenue() != null || e2.getVenue() != null) && (e1.getDate() != null && e2.getDate() != null) ){
      // We have date and venue
      if ( e1.getVenue() instanceof List && e2.getVenue() instanceof  List)
        venue_sim = this.tools.geoPolygonSimilarity((List)e1.getVenue(), (List)e2.getVenue());

      if ( e1.getVenue() instanceof GeoPoint && e2.getVenue() instanceof  GeoPoint)
        venue_sim = this.tools.geoPointSimilarity((GeoPoint)e1.getVenue(), (GeoPoint)e2.getVenue());

      if ( e1.getVenue() instanceof GeoPoint && e2.getVenue() instanceof List)
        venue_sim = this.tools.pointPolygonSimilarity((GeoPoint)e1.getVenue(), (List)e2.getVenue());

      if ( e2.getVenue() instanceof GeoPoint && e1.getVenue() instanceof List)
        venue_sim = this.tools.pointPolygonSimilarity((GeoPoint)e2.getVenue(), (List)e1.getVenue());

      return this.tools.dateSimilarity(e1.getDate(), e2.getDate());
    }

    return 0.0;
    */
  }
}
