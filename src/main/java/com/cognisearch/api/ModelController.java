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

/**
 * Created by armel on 17/01/18.
 */

@Path("/models")
public class ModelController {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ModelController.class);

  private  EsWorker worker = new EsWorker("localhost", 9300, "cognisearch_event_20171231", "event_type", "resources/categoriesEvt.xml");

  @GET
  @Path("/places")
  @Produces(MediaType.APPLICATION_JSON)
  public com.cogniteev.cognisearch.event.model.PlaceList getPlaces(){

    return this.worker.getAllPlaces();
  }


  @GET
  @Path("/categories")
  @Produces(MediaType.APPLICATION_JSON)
  public com.cogniteev.cognisearch.event.model.CatList getCategories()throws IOException, SAXException, ParserConfigurationException {

    return this.worker.getAllCategories();
  }

  @GET
  @Path("/performers")
  @Produces(MediaType.APPLICATION_JSON)
  public com.cogniteev.cognisearch.event.model.CatList getPerformers()throws IOException, SAXException, ParserConfigurationException {

    return this.worker.getAllPerformers();
  }
}
