/**
 * 
 */
package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval.team16;

import java.io.IOException;
import java.net.URL;
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
 * @author team16
 * 
 */
public class NormalSynonymProvider {
  /**
   * offset is used to track which word in the keyterm list should be
   * take to change the query. It increases every time the reformWithSynonym 
   * method is being called.
   */
  private int offset = 0;

  /**
   * 
   * 
   * 
   * @param keyterms
   * @param query
   * @return
   */
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
  
  /**
   * this is similar to reformWithSynonym, but we add the original word back to form
   * a complete query expansion.
   * 
   * @param keyterms
   * @param query
   * @return
   */
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

  /**
   * 
   * getSynonyms will return you a list of synonyms give the word and the maxmum
   * number of synonyms you want.
   * 
   * Special: synCount = 0 will return everything it finds out.
   * 
   * @param originalWord
   * @param synCount
   * @return
   */
  private static List<String> getSynonyms(String originalWord, int synCount) {
    URL url = null;
    List<String> synList = new ArrayList<String>();
    IDictionary dict = null;
    int count = 0;
    try {
      url = new URL("file", null, "/usr4/ziy/tmp/hw2-eval/wordnet-dict");
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
        String synonym = w.getLemma().replaceAll("_", " "); // replace "_" with ""
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
