/**
 * 
 */
package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval;

/**
 * @author team16
 *
 * This class is designed for all kinds of Operator changes.
 * Currently it only contains AND => OR.
 * We tried to use SDM (sequencial dependency model) before, but it did not show improvement.
 */
public class OperatorSpecialist {
  /**
   * replace the first AND with OR
   * 
   * @param query
   * @return
   */
  public static String changeOperator(String query){
    query = query.replaceFirst("AND", "OR");
    return query;
  }
}
