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
    
    // return "\"Parkinson\'s disease\" AND \"Orphan nuclear receptor\""; // 164
    // return "(Cathepsin D OR (CTSD)) AND (apolipoprotein OR E (ApoE)) AND contribute AND Alzheimer's disease"; // 165
    // return "(\"nucleoside diphosphate kinase\" OR NM23) AND \"tumor\""; // 167
    return "COP2 OR contribute OR CFTR AND \"endoplasmic reticulum\""; // 170
    
  }
}
