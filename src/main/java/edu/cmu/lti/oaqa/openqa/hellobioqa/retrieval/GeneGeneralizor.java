/**
 * 
 */
package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval;

import java.util.List;

import edu.cmu.lti.oaqa.framework.data.Keyterm;

/**
 * @author mingtaozhang
 * generalize gene
 * Nurr-77 => "Orphan nuclear receptor" 
 */
public class GeneGeneralizor {
  public static String generalizeGene(List<Keyterm> keyterms, String query){
    // TODO gene generalization
    
    return "\"Parkinson\'s disease\" AND \"Orphan nuclear receptor\"";
  }
}
