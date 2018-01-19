package com.cognisearch.api;

import com.cognisearch.helpers.*;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Object;
import java.lang.String;
import java.util.*;
import java.util.ArrayList;
import com.cogniteev.cognisearch.event.model.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;
import com.cogniteev.cognisearch.event.model.ResultsList;

import org.xml.sax.SAXException;

import org.elasticsearch.common.geo.*;

/**
 * Created by armel on 07/01/18.
 */

@Path("/")
public class SimpleSearch {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SimpleSearch.class);

  @GET
  @Path("/home")
  @Produces(MediaType.APPLICATION_JSON)
  public List<com.cogniteev.cognisearch.event.model.Result> test(){
    com.cogniteev.cognisearch.event.model.Result res = new com.cogniteev.cognisearch.event.model.Result();
    res.setName("Albert");
    return Arrays.asList(res);
  }

  @GET
  @Path("/search/simple")
  @Produces(MediaType.APPLICATION_JSON)

  public com.cogniteev.cognisearch.event.model.ResultsList basicSearch(@QueryParam(value = "category") String categories,
  //public String basicSearch(@QueryParam(value = "category") List<String> categories,
                                              @QueryParam(value = "performers") String performers,
                                              @QueryParam(value = "venue") String points,
                                              @QueryParam(value = "date") String date)
             throws ParserConfigurationException, SAXException, IOException
  {

    com.cogniteev.cognisearch.event.model.ResultsList res = new com.cogniteev.cognisearch.event.model.ResultsList();

    EsWorker worker = new EsWorker("localhost", 9300, "cognisearch_event_20171231", "event_type", "resources/categoriesEvt.xml");
    LOG.info(categories);
    LOG.info(points);
    LOG.info(date);
    LOG.info(performers);

    // Creating the paramater Object
    Map<String, Object> params = new HashMap<>();

    if (categories != null){
      List<String> cats = new ArrayList<String>();
      String[] catsString = categories.split("@");

      for(String c : catsString)
        cats.add(c.replace("$", "#"));

      params.put("category", cats);
    }

    if (performers != null){
      List<String> perfs = new ArrayList<String>();
      String[] perfsString = performers.split("@");

      for(String p : perfsString)
        perfs.add(p);

      params.put("performers", perfs);
    }

    if (date != null)
      params.put("date", date);

    if ( points != null) {
      List<GeoPoint> venue = new ArrayList<>();

      String[] pointsString = points.split("@");

      for( String p : pointsString){
        String[] latLong = p.split("M");

        if (latLong.length < 2)
          return null;

        double lat = Double.parseDouble(latLong[0]);
        double lng = Double.parseDouble(latLong[1]);

        venue.add(new GeoPoint(lat, lng));
      }

      params.put("venue", venue);
    }


      res = worker.simpleSearch(params);
    /*}
    catch (Exception ex){
      LOG.error(ex.getMessage());
      com.cogniteev.cognisearch.event.model.Result obj = new com.cogniteev.cognisearch.event.model.Result();
      obj.setName(ex.getMessage());
      res.setResults(Arrays.asList(obj));
      return res;
    }*/
    return res;
    //return res.get(0).getName();
  }
}