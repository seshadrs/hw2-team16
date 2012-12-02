/**
 * 
 */
package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.lti.oaqa.framework.data.Keyterm;

/**
 * @author mingtaozhang
 * 
 */
public class SynonymProvider {

  final static String link = "http://words.bighugelabs.com/api/2/617c51f37fdfd2744d58884123d4bc6c/";

  public static String reformWithSynonym(List<Keyterm> keyterms, String query){
    return null;
  }
  
  public static List<String> getSynonyms(String word, int n) {
    List<String> result = new ArrayList<String>();
    try {
      URL url = new URL(link + word + "/");
      URLConnection con = url.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;
      while ((inputLine = in.readLine()) != null && n > 0) {
        result.add(inputLine.substring(inputLine.lastIndexOf("|") + 1));
        n--;
      }
      in.close();
    } catch (Exception e) {
      return null;
    }
    return result;
  }

  public static void main(String[] args) {
    System.out.println(getSynonyms("Parkinson's disease", 2));
    System.out.println(getSynonyms("Nurr-77", 2));
  }
}
