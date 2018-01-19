package com.cogniteev.cognisearch.event.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by armel on 07/12/16.
 */
public class Price {
  private double maxPrice;

  public double getMinPrice() {
    return minPrice;
  }

  public void setMinPrice(double minPrice) {
    this.minPrice = minPrice;
  }

  public double getMaxPrice() {
    return maxPrice;
  }

  public void setMaxPrice(double maxPrice) {
    this.maxPrice = maxPrice;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  private double minPrice;
  private String currency;

  public String toString(){
    if(maxPrice != 0.0)
      return minPrice + currency + " - " + maxPrice +  currency + "\n";
    else
      return minPrice +  currency + "\n";
  }


  /**
   * This function parses a Price Object and returns the Map object corresponding to it
   * @return
   */
  public Map<String, Object> toMap()
  {
    Map<String, Object> price = new HashMap<>();

    if ( this.getMinPrice() != 0.0 )
      price.put("minPrice", this.getMinPrice());

    if ( this.getMaxPrice() != 0.0 )
      price.put("maxPrice", this.getMaxPrice());

    if ( this.getCurrency() != null )
      price.put("currency", this.getCurrency());


    return price;
  }
}
