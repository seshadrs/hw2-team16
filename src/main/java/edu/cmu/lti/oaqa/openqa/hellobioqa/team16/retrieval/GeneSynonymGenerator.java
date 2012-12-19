/**
 * 
 */
package edu.cmu.lti.oaqa.openqa.hellobioqa.team16.retrieval;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.lti.oaqa.framework.data.Keyterm;

/**
 * @author team16
 * this class generate gene synonyms by talking with a gene database 
 *   
 */
public class GeneSynonymGenerator {
  /**
   * offset is used to track which word in the keyterm list should be
   * take to change the query. It increases every time the generalizeGene 
   * method is being called.
   */
  private int offset = 0;
  
  public String generalizeGene(List<Keyterm> keyterms, String query){
    while(offset < keyterms.size() && keyterms.get(offset).getProbability() != 1){
      offset++;
    }
    if(offset == keyterms.size()) return query;
    List<String> genes = getGeneFamily(keyterms.get(offset).getText());
    if(genes != null){
      StringBuilder temp = new StringBuilder();
      temp.append("(");
      for(String gene : genes){
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
   * similar with the previous methods, but include the current keyterm
   * 
   * @param keyterms
   * @param query
   * @return
   */
  public String generalizeGeneForOR(List<Keyterm> keyterms, String query){
    while(offset < keyterms.size() && keyterms.get(offset).getProbability() != 1){
      offset++;
    }
    if(offset == keyterms.size()) return query;
    List<String> genes = getGeneFamily(keyterms.get(offset).getText());
    if(genes != null){
      StringBuilder temp = new StringBuilder();
      temp.append("(");
      temp.append(keyterms.get(offset).getText()).append(" OR ");
      for(String gene : genes){
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
   * getGeneFamily will return a list of related genes given a gene
   * 
   * @param word
   * @return
   */
  public List<String> getGeneFamily(String word) {
    List<String> result = new ArrayList<String>();
    List<String> geneSyn = GeneSynonymDatabase.searchGeneSynonyms(word);
    if(geneSyn == null) return null;
    for (String synonym : geneSyn){
      if (synonym.equals(""))
        continue;
      result.add(synonym);
    }
    return result;
  }
}
