/**
 * 
 */
package edu.cmu.lti.oaqa.openqa.hellobioqa.inhancement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mingtaozhang
 * 
 */
public class SynonymProvider {

  final static String link = "http://words.bighugelabs.com/api/2/0532e84c61e3406c66e209adc30ebe16/";

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
    System.out.println(getSynonyms("Parkinson disease", 2));
  }
}
