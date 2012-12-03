/**
 * 
 */
package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval;

/**
 * @author mingtaozhang
 *
 */
public class OperatorSpecialist {
  public static String changeOperator(String query){
    query = query.replaceFirst("AND", "OR");
    return query;
  }
}
