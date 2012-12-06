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

  int offset = 0;

  public String reformWithSynonym(List<Keyterm> keyterms, String query) {
    List<String> syns = getSynonyms(keyterms.get(offset).getText(), 3);
    if (syns != null) {
      StringBuilder temp = new StringBuilder();
      temp.append("(");
      for(String gene : syns){
        temp.append(gene).append(" OR ");
      }
      String result = temp.toString();
      result = result.substring(0, result.length() - "OR".length() - 2)+")";
      query = query.replaceFirst(keyterms.get(offset).getText(), result);
    }
    offset++;
    return query;
  }
  
  public String reformWithSynonymForOR(List<Keyterm> keyterms, String query) {
    List<String> syns = getSynonyms(keyterms.get(offset).getText(), 3);
    if (syns != null) {
      StringBuilder temp = new StringBuilder();
      temp.append("(");
      temp.append(keyterms.get(offset).getText()).append(" OR ");
      for(String gene : syns){
        temp.append(gene).append(" OR ");
      }
      String result = temp.toString();
      result = result.substring(0, result.length() - "OR".length() - 2)+")";
      query = query.replaceFirst(keyterms.get(offset).getText(), result);
    }
    offset++;
    return query;
  }

  private static List<String> getSynonyms(String originalWord, int synCount) {
    URL url = null;
    List<String> synList = new ArrayList<String>();
    IDictionary dict = null;
    int count = 0;
    try {
      url = new URL("file", null, "dict");
      dict = new Dictionary(url);
      dict.open();
    } catch (IOException e) {
      e.printStackTrace();
    }
    IIndexWord idxWord = dict.getIndexWord(originalWord, POS.NOUN);
    // Find the first meaning of the word
    if(idxWord !=null){
      IWordID wordID = idxWord.getWordIDs().get(0);
      IWord word = dict.getWord(wordID);
      ISynset synset = word.getSynset();
      for (IWord w : synset.getWords()) {
        // replace "_" with ""
        String synonym = w.getLemma().replaceAll("_", " ");
        synList.add(synonym);
        count++;
        if(count == synCount)
          break;
      }
    } else {
      return null;
    }
    return synList;
  }
}
