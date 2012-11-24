/**
 * 
 */
package edu.cmu.lti.oaqa.openqa.hellobioqa.inhancement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mingtaozhang
 *
 */
public class GoldenKeytermsProvider {
  private static BufferedReader reader;
  
  public static List<List<String>> goldenTerms;
  
  public static int index = 0;
  
  private GoldenKeytermsProvider(){}
  
  // static constructor
  static {
    try {
      // for testing
      reader = new BufferedReader(new FileReader(new File("src/main/resources/gs/trecgen06.keyterm")));
      goldenTerms = new ArrayList<List<String>>();
      List<String> tempTerms = new ArrayList<String>();
      String line;
      int id = 0;
      while((line = reader.readLine()) != null){
        int queryid = Integer.parseInt(line.substring(0, line.indexOf("|")));
        if(id == 0){
          // first time
          id = queryid;
          tempTerms.add(line.substring(line.lastIndexOf("|") + 1));
        } else {
          if(id != queryid){
            // a new question's keyterm
            goldenTerms.add(tempTerms);
            tempTerms = new ArrayList<String>();
            tempTerms.add(line.substring(line.lastIndexOf("|") + 1));
            id = queryid;
          } else {
            tempTerms.add(line.substring(line.lastIndexOf("|") + 1));
          }
        }
      }
      goldenTerms.add(tempTerms);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  public static List<String> getGoldenKeyterms(String question){
    List<String> result = new ArrayList<String>();
    int maxMatch = 0;
    for(List<String> keytermsPerQuestion : goldenTerms){
      int match = 0;
      for(String keyterm : keytermsPerQuestion){
        if(question.contains(keyterm)){
          match++;
        }
      }
      if(match > maxMatch){
        result = keytermsPerQuestion;
        maxMatch = match;
      }
    }
    return result;
  } 
  
  public static String getGoldenQuery(){
    StringBuffer result = new StringBuffer();
    List<String> keyterms = goldenTerms.get(index);
    for(String keyterm : keyterms){
      result.append(keyterm + " ");
    }
    index++;
    return result.toString();
  } 

  /**
   * for testing purpose
   * 
   * @param args
   */
  public static void main(String[] args){
    for(int i=0;i < 188-160 ; i++){
      System.out.println(getGoldenQuery());
    }
  }
}
