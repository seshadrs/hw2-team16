/**
 * 
 */
package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.lti.oaqa.framework.data.Keyterm;

/**
 * @author mingtaozhang
 * generalize gene
 * Nurr-77 => "Orphan nuclear receptor"
 * //TODO gene generalization
  // return "\"Parkinson\'s disease\" AND \"Orphan nuclear receptor\""; // 164
  // return "(Cathepsin D OR (CTSD)) AND (apolipoprotein OR E (ApoE)) AND contribute AND Alzheimer's disease"; // 165
  // return "(\"nucleoside diphosphate kinase\" OR NM23) AND \"tumor\""; // 167
  // return "COP2 OR contribute OR CFTR AND \"endoplasmic reticulum\""; // 170 
 */
public class GeneGeneralizor {
  int offset = 0;
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
  
  public List<String> getGeneFamily(String word) {
    List<String> result = new ArrayList<String>();
    List<String> geneSyn = GeneNameDatabase.searchGeneSynonyms(word);
    if(geneSyn == null) return null;
    for (String synonym : geneSyn){
      if (synonym.equals(""))
        continue;
      result.add(synonym);
    }
    return result;
  }
}
