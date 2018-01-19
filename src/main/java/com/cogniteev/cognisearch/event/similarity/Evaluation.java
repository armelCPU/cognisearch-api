package com.cogniteev.cognisearch.event.similarity;

import au.com.bytecode.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by armel on 26/06/17.
 */
public class Evaluation {
  private double[] expert_scores;
  private double[] system_scores;
  private int system_column;
  private double threshold;
  private int size = 100;
  private int expert_column;

  private int VP = 0;
  private int VN = 0;
  private int FP = 0;
  private int FN = 0;

  private double P;
  private double R;
  private double F;

  public Evaluation(int system_column, int expert_column,  double threshold){
    this.system_column = system_column;
    this.expert_column = expert_column;
    this.threshold = threshold;
  }

  public double accuracy(){
    return (double)(VP + VN)/(VP + FP + FN + VN);
  }

  public double precision(){
    return (double)VP/(VP + FP);
  }

  public double rappel(){
    return (double)VP/(VP + FN);
  }

  public double f_mesure(){
    return (2*P*R)/(P + R);
  }

  public void buildVectors(String file_name) throws IOException {
    // Reading the file
    File inputFile = new File(file_name);

    // Read existing file
    CSVReader reader = new CSVReader(new FileReader(inputFile), '\t');
    List<String[]> csvBody = reader.readAll();

    // Matrix
    this.size = csvBody.size();

    expert_scores = new double[this.size];
    system_scores = new double[this.size];

    int i = 0;
    for ( String[] line : csvBody){
      if (i == 0) {
        i++;
        continue;
      }
      system_scores[i-1] = Double.parseDouble(line[this.system_column].replace(",", "."));
      expert_scores[i-1] = Double.parseDouble(line[this.expert_column].replace(",", "."));
      i++;
    }

  }

  private void calculeMetrics(String file_name) throws IOException {
    this.buildVectors(file_name);

    for(int i=0; i<this.size; i++){
      if ( this.expert_scores[i] == 1.0 && this.system_scores[i] >  this.threshold)
        VP ++;

      if ( this.expert_scores[i] == 0.0 && this.system_scores[i] > this.threshold){
        FP ++;
      }


      if ( this.expert_scores[i] == 1.0 && this.system_scores[i] <= this.threshold)
        FN ++;

      if ( this.expert_scores[i] == 0.0 && this.system_scores[i] <= this.threshold)
        VN ++;
    }

    System.out.println("VP : " + this.VP);
    System.out.println("VN : " + this.VN);
    System.out.println("FP : " + this.FP);
    System.out.println("FN : " + this.FN);

    P = this.precision();
    R = this.rappel();

    System.out.println("Precision : " + this.precision());
    System.out.println("Rappel : " + this.rappel());
    System.out.println("F1-mesure : " + this.f_mesure());
    System.out.println("ACCURACY : " + this.accuracy());
  }

  public static void main(String[] args) throws Exception {
    //String filename = "resources/test/Rep/cat5.csv";
    String filename = "resources/dataset.csv";

    //Evaluation eval = new Evaluation(5, 3, 0.67);
    //Evaluation eval = new Evaluation(6, 3, 0.64);
    Evaluation eval = new Evaluation(7, 3, 0.68);
    //Evaluation eval = new Evaluation(8, 3, 0.79);
    //Evaluation eval = new Evaluation(9, 3, 0.64);

    eval.calculeMetrics(filename);
  }
}
