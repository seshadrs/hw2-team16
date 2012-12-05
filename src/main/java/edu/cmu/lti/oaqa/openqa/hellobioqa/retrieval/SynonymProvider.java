/**
 * 
 */
package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

/**
 * @author mingtaozhang
 * 
 */
public class SynonymProvider {

  final static String link = "http://words.bighugelabs.com/api/2/617c51f37fdfd2744d58884123d4bc6c/";

  int offset = 0;

  public String reformWithSynonym(List<Keyterm> keyterms, String query) {
    List<String> temp = getSynonyms(keyterms.get(offset).getText(), 1);
    if (temp != null) {
      query = query.replaceFirst(keyterms.get(offset).getText(), temp.get(0));
    }
    offset++;
    return query;
  }

  public List<String> getSynonyms(String word, int n) {
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

  private static List<String> lookUpWordNet(String originalWord, int synCount) {
    URL url = null;
    int count = 0;
    try {
      url = new URL("file", null, "dict");
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    List<String> synList = new ArrayList<String>();

    IDictionary dict = new Dictionary(url);
    try {
      dict.open();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    IIndexWord idxWord = dict.getIndexWord(originalWord, POS.NOUN);
    // Find the first meaning of the word
    IWordID wordID = idxWord.getWordIDs().get(0);
    IWord word = dict.getWord(wordID);
    ISynset synset = word.getSynset();
    for (IWord w : synset.getWords()) {
      String synonym = w.getLemma();
      // replace "_" with ""
      synonym = synonym.replaceAll("_", " ");
      System.out.println(synonym);
      synList.add(synonym);
      count++;
      if(count == synCount)
        break;
    }
    return synList;
  }

  public static void main(String[] args) {
    try {
      lookUpWordNet("Parkinson's disease", 7);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
