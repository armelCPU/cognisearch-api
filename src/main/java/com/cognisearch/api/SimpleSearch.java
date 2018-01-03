package com.cognisearch.api;


import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.*;

/**
 * Created by armel on 07/01/18.
 */

@Path("/")
public class SimpleSearch {

  @GET
  @Path("/home")
  @Produces(MediaType.APPLICATION_JSON)

  public String test(){
    return "Test";
  }
}