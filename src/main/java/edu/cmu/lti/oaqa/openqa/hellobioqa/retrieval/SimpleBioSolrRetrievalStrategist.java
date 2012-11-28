/*
 *  Copyright 2012 Carnegie Mellon University
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.cse.basephase.retrieval.AbstractRetrievalStrategist;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;
import edu.cmu.lti.oaqa.openqa.hellobioqa.inhancement.SynonymProvider;

/**
 * 
 * @author Zi Yang <ziy@cs.cmu.edu>
 * 
 */
public class SimpleBioSolrRetrievalStrategist extends AbstractRetrievalStrategist {

  protected int hitListSize;

  protected double singleWordWeight;
  
  protected double nearWeight;
  
  protected double geneWeight;
  
  protected int numberOfSynonym;
  
  protected double threshold;
  
  protected double synonymWeight;

  protected int dependencyLength;
  
  protected SolrWrapper wrapper;
  
  // private int combinationWeight = 4; 

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    try {
      this.hitListSize = (Integer) (aContext.getConfigParameterValue("hit-list-size"));
    } catch (ClassCastException e) { 
      // all cross-opts are strings?
      this.hitListSize = Integer.parseInt((String) aContext.getConfigParameterValue("hit-list-size"));
    }
    try {
      this.numberOfSynonym = (Integer) (aContext.getConfigParameterValue("numberOfSynonym"));
    } catch (ClassCastException e) { 
      // all cross-opts are strings?
      this.numberOfSynonym = Integer.parseInt((String) aContext.getConfigParameterValue("numberOfSynonym"));
    }
    try {
      this.singleWordWeight = (Double) (aContext.getConfigParameterValue("singleWordWeight"));
    } catch (ClassCastException e) { 
      // all cross-opts are strings?
      this.singleWordWeight = Double.parseDouble((String) aContext.getConfigParameterValue("singleWordWeight"));
    }
    try {
      this.geneWeight = (Double) (aContext.getConfigParameterValue("geneWeight"));
    } catch (ClassCastException e) { 
      // all cross-opts are strings?
      this.geneWeight = Double.parseDouble((String) aContext.getConfigParameterValue("geneWeight"));
    }
    try {
      this.nearWeight = (Double) (aContext.getConfigParameterValue("nearWeight"));
    } catch (ClassCastException e) { 
      // all cross-opts are strings?
      this.nearWeight = Double.parseDouble((String) aContext.getConfigParameterValue("nearWeight"));
    }
    try {
      this.dependencyLength = (Integer) (aContext.getConfigParameterValue("dependencyLength"));
    } catch (ClassCastException e) { 
      // all cross-opts are strings?
      this.dependencyLength = Integer.parseInt((String) aContext.getConfigParameterValue("dependencyLength"));
    }
    try {
      this.synonymWeight = (Float) (aContext.getConfigParameterValue("synonymWeight"));
    } catch (ClassCastException e) { 
      // all cross-opts are strings?
      this.synonymWeight = Float.parseFloat((String) aContext.getConfigParameterValue("synonymWeight"));
    }
    try {
      this.threshold = (Float) (aContext.getConfigParameterValue("threshold"));
    } catch (ClassCastException e) { 
      // all cross-opts are strings?
      this.threshold = Float.parseFloat((String) aContext.getConfigParameterValue("threshold"));
    }
    String serverUrl = (String) aContext.getConfigParameterValue("server");
    Integer serverPort = (Integer) aContext.getConfigParameterValue("port");
    Boolean embedded = (Boolean) aContext.getConfigParameterValue("embedded");
    String core = (String) aContext.getConfigParameterValue("core");
    try {
      this.wrapper = new SolrWrapper(serverUrl, serverPort, embedded, core);
    } catch (Exception e) {
      // e.printStackTrace();
      throw new ResourceInitializationException(e);
    }
  }

  @Override
  protected final List<RetrievalResult> retrieveDocuments(String questionText,
          List<Keyterm> keyterms) {    
    String query = formulateQuery(keyterms);
    // query = query + " " + queryFromQuestion(questionText);
    System.out.println("Query: " + query);
    return retrieveDocuments(query);
  }

  private String queryFromQuestion(String questionText) {
    return questionText.replace("?", "");
  }

  protected String formulateQuery(List<Keyterm> keyterms) {
    StringBuffer result = new StringBuffer();
    for(Keyterm term : keyterms){
      if(term.getProbability() == 0){
        // not a gene
        if(term.getText().contains(" ")){
          result.append("\"" + term.getText() + "\" AND ");
        } else {
          result.append(term.getText() + "^" + singleWordWeight + " AND ");
        }
      } else {
        // is a gene
        if(term.getText().contains(" ")){
          result.append("\"" + term.getText() + "\"^" + geneWeight + " AND ");
        } else {
          result.append(term.getText() + "^" + geneWeight + " AND ");
        }
      }
    }
    /*
    for (Keyterm keyterm : keyterms) {
      String temp = keyterm.getText();
      List<String> t = SynonymProvider.getSynonyms(keyterm.getText(), numberOfSynonym);
      if(t != null && temp.contains(" ")) {//TODO
        result.append("(");
        for(String syn: t){
          if (syn.contains(" ")) {
            result.append(near(syn) + "^"+nearWeight+" AND ");
            result.append(and(syn) + " AND ");
          } else {
            result.append(syn + "^"+singleWordWeight+" AND ");
          }
        }
        result.delete(result.length() - 5, result.length());
        result.append(")^"+synonymWeight+" AND ");
      }
      if (temp.contains(" ")) {
        result.append(near(temp) + "^"+nearWeight+" AND ");
        result.append(and(temp) + " AND ");
      } else {
        result.append(temp + "^"+singleWordWeight+" AND ");
      }
    }
    */
    String query = result.toString().trim();
    query = query.substring(0, query.length() - 4);
    return query;
  }

  /*
  private String near(String relatedTerms) {
    StringBuilder result = new StringBuilder();
    String[] terms = relatedTerms.split(" ");
    int step = 2;
    while (step <= terms.length) {
      String[] temp = new String[terms.length - step + 1];
      for (int i = 0; i < temp.length ; i++) {
        temp[i] = "";
      }
      for (int j = 0; j < terms.length - step + 1; j++) {
        for (int i = 0; i < terms.length; i++) {
          if (i >= j && i < j + step) {
            temp[j] = temp[j] + terms[i]+" ";
          }
        }
      }
      for (String t : temp) {
        t = "\"" + t.trim() + "\"~" + dependencyLength * (step - 1) + " OR ";
        result.append(t);
      }
      step++;
    }
    String temp = result.toString().trim();
    return "("+temp.substring(0, temp.length() - 3)+")";
  }
  
  private String and(String relatedTerms) {
    StringBuilder result = new StringBuilder();
    String[] terms = relatedTerms.split(" ");
    int step = 2;
    while (step <= terms.length) {
      String[] temp = new String[terms.length - step + 1];
      for (int i = 0; i < temp.length ; i++) {
        temp[i] = "";
      }
      for (int j = 0; j < terms.length - step + 1; j++) {
        for (int i = 0; i < terms.length; i++) {
          if (i >= j && i < j + step) {
            temp[j] = temp[j] + terms[i] + " AND ";
          }
        }
      }
      for (String t : temp) {
        String tem = t.trim();
        t = "("+ tem.substring(0, tem.length() - 4)+ ") OR ";
        result.append(t);
      }
      step++;
    }
    String temp = result.toString().trim();
    return "("+temp.substring(0, temp.length() - 3)+")";
  }
  */

  private List<RetrievalResult> retrieveDocuments(String query) {
    List<RetrievalResult> result = new ArrayList<RetrievalResult>();
    try {
      SolrDocumentList docs = wrapper.runQuery(query, hitListSize);
      for (SolrDocument doc : docs) {
        RetrievalResult r = new RetrievalResult((String) doc.getFieldValue("id"),
                (Float) doc.getFieldValue("score"), query);
        if ((Float) doc.getFieldValue("score") < threshold)
          break;
        result.add(r);
        System.out.println(doc.getFieldValue("id"));
      }
    } catch (Exception e) {
      System.err.println("Error retrieving documents from Solr: " + e);
    }
    return result;
  }

  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    super.collectionProcessComplete();
    wrapper.close();
  }
}