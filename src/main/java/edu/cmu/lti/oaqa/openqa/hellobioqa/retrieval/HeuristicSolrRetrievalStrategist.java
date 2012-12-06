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

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.core.provider.solr.SolrWrapper;
import edu.cmu.lti.oaqa.cse.basephase.retrieval.AbstractRetrievalStrategist;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;

/**
 * 
 * @author team16
 * 
 * This class is designed to complete the retrieval process
 */
public class HeuristicSolrRetrievalStrategist extends AbstractRetrievalStrategist {

  private String dupQuery;
  
  protected int minimumResult;
  
  private List<Keyterm> keyterms;
  
  protected SolrWrapper wrapper;
  
  protected String operator = "AND";
  
  protected double singleWordWeight;
  
  protected int hitListSize;  
  
  protected double geneWeight; 

  /**
   * initialize this component
   * 
   */
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
    minimumResult = this.hitListSize / 10;
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

  /**
   * retrievaDocument will call two sub methods to completet the retrieval process
   * 
   */
  @Override
  protected final List<RetrievalResult> retrieveDocuments(String questionText,
          List<Keyterm> keyterms) {
    this.keyterms = keyterms; // for gene generalization purpose
    String query = formulateTheInitialQuery(keyterms);
    return retrieveDocuments(query);
  }

  /**
   * form the orinal query by combining the keyterms.
   * 
   * If it's a gene, we add gene weight on the right
   * If it's not a gene, we add normal weight on the right
   * 
   * We add "..phrase.." to combine a phrase tightly.
   * 
   * We add singleWordWeight to control the weihgt between single word and phrase
   * 
   * @param keyterms
   * @return
   */
  protected String formulateTheInitialQuery(List<Keyterm> keyterms) {
    StringBuffer result = new StringBuffer();
    for(Keyterm term : keyterms){
      if(term.getProbability() == 0){
        // not a gene
        if(term.getText().contains(" ")){
          result.append("\"" + term.getText() + "\" "+operator+" ");
        } else {
          result.append(term.getText() + "^" + singleWordWeight + " "+operator+" ");
        }
      } else {
        // is a gene
        if(term.getText().contains(" ")){
          result.append("\"" + term.getText() + "\"^" + geneWeight + " "+operator+" ");
        } else {
          result.append(term.getText() + "^" + geneWeight + " "+operator+" ");
        }
      }
    }
    String query = result.toString().trim();
    query = query.substring(0, query.length() - operator.length() - 1);
    return query;
  }

  /**
   * call the solr wrapper to execute the query
   * 
   * It prevent wasting searching resources by noting down duplicate queries.
   * 
   * @param query
   * @param hitListSize
   * @return
   */
  private SolrDocumentList runQuery(String query, int hitListSize){
    if(this.dupQuery == null){
      this.dupQuery = query;
      try {
        return wrapper.runQuery(query, hitListSize);
      } catch (SolrServerException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      if(!this.dupQuery.equals(query)){
        try {
          System.out.println(query);
          this.dupQuery = query;
          return wrapper.runQuery(query, hitListSize);
        } catch (SolrServerException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }  
  
  /**
   * retrieve Documnet will do 
   * 1. synonym expansion
   * 2. gene expansion
   * 3. increase recall by changing AND to OR one by one.
   * 
   * It accumulates the results from the above 3 steps.
   * 
   * @param query
   * @return
   */
  private List<RetrievalResult> retrieveDocuments(String query) {
    String originalQuery = query;
    String newQuery = query;
    List<RetrievalResult> result = new ArrayList<RetrievalResult>();
    try {
      SolrDocumentList docs = runQuery(newQuery, hitListSize);
      int temp = 0;
      NormalSynonymProvider syn = new NormalSynonymProvider();
      while(docs.size() < this.minimumResult && temp < keyterms.size() - 1){
        // do synonym expansion 
        newQuery = syn.reformWithSynonym(this.keyterms, originalQuery);
        SolrDocumentList tempDocs = runQuery(newQuery, hitListSize);
        SolrDocumentList duplicate = new SolrDocumentList();
        if(tempDocs != null){
          for(SolrDocument sall : docs){
            for(SolrDocument stemp : tempDocs){
              if(stemp.getFieldValue("id").equals(sall.getFieldValue("id"))){
                duplicate.add(stemp);
              }
            }
          }
          docs.addAll(tempDocs);
          docs.removeAll(duplicate);
        }
        temp++;
      }
      // reset
      newQuery = originalQuery;
      temp = 0;
      // do gene expansion
      GeneSynonymGenerator geneGen = new GeneSynonymGenerator();
      while(docs.size() < this.minimumResult && temp < keyterms.size() - 1){
        newQuery = geneGen.generalizeGene(this.keyterms, originalQuery);
        SolrDocumentList tempDocs = runQuery(newQuery, hitListSize);
        SolrDocumentList duplicate = new SolrDocumentList();
        if(tempDocs != null){
          for(SolrDocument sall : docs){
            for(SolrDocument stemp : tempDocs){
              if(stemp.getFieldValue("id").equals(sall.getFieldValue("id"))){
                duplicate.add(stemp);
              }
            }
          }
          docs.addAll(tempDocs);
          docs.removeAll(duplicate);
        }
        temp++;
      }
      // do the expansion and synonym
      temp = 0;
      syn = new NormalSynonymProvider();
      while(temp < keyterms.size() - 1){ 
        newQuery = syn.reformWithSynonymForOR(this.keyterms, newQuery);
        temp++;
      }
      temp = 0;
      geneGen = new GeneSynonymGenerator();
      while(temp < keyterms.size() - 1){
        newQuery = geneGen.generalizeGeneForOR(this.keyterms, newQuery);
        temp++;
      }
      temp = 0;
      while(docs.size() < this.minimumResult && temp < keyterms.size() - 1){
        // do AND -> OR replace
        newQuery = OperatorSpecialist.changeOperator(newQuery);
        SolrDocumentList tempDocs = runQuery(newQuery, hitListSize);
        SolrDocumentList duplicate = new SolrDocumentList();
        if(tempDocs != null){
          for(SolrDocument sall : docs){
            for(SolrDocument stemp : tempDocs){
              if(stemp.getFieldValue("id").equals(sall.getFieldValue("id"))){
                duplicate.add(stemp);
              }
            }
          }
          docs.addAll(tempDocs);
          docs.removeAll(duplicate);
        }
        temp++;
      }      
      // add the result into result set
      for (SolrDocument doc : docs) {
        RetrievalResult r = new RetrievalResult((String) doc.getFieldValue("id"),
                (Float) doc.getFieldValue("score"), query);
        boolean duplicate = false;
        for(RetrievalResult rr : result){
          if(rr.getDocID() == r.getDocID()) {
            duplicate = true;
            break;
          }
        }
        if(!duplicate){
          result.add(r);
          System.out.println(doc.getFieldValue("id"));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Error retrieving documents from Solr: " + e);
    }
    return result;
  }

  /**
   * fit into the framework
   * 
   */
  @Override
  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    super.collectionProcessComplete();
    wrapper.close();
  }
}