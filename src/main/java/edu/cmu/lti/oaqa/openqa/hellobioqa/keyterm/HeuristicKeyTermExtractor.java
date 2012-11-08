package edu.cmu.lti.oaqa.openqa.hellobioqa.keyterm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.UimaContext;
import org.apache.uima.pear.util.StringUtil;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;


public class HeuristicKeyTermExtractor extends AbstractKeytermExtractor{
  
  private PosTagNamedEntityRecognizer nounPhraseAnnotator;

  
  
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
   
    try {
      nounPhraseAnnotator=new PosTagNamedEntityRecognizer();
    } catch (ResourceInitializationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  @Override
  protected List<Keyterm> getKeyterms(String context) {
    // TODO Auto-generated method stub
    List<Keyterm> keyterms = new ArrayList<Keyterm>();
    
    System.out.println("%%%%%%%%%%%%111111111111111111%%%%%%%%%%%%%%");
    Map<Integer,Integer> geneCandidates = nounPhraseAnnotator.getGeneSpans(context);
    Double totalScore = 0.0;
    Map<String,Double> candidateScores= new HashMap<String,Double>();
    
    for (Map.Entry<Integer, Integer> entry : geneCandidates.entrySet())
    {
        System.out.println(entry.getKey() + "-" + entry.getValue());
        System.out.println(context.substring(entry.getKey(), entry.getValue()));
        
        String candidate = context.substring(entry.getKey(), entry.getValue());
        double candidateScore = HeuristicScorer.getScore(candidate);
        candidateScores.put(candidate, candidateScore);
        totalScore += candidateScore;
    }
    
    Double avgScore = totalScore/((double)candidateScores.size());
    
    for (Map.Entry<String, Double> entry : candidateScores.entrySet())
    {
      if (entry.getValue() >=avgScore)
        keyterms.add(new Keyterm(entry.getKey()));
    }
    
    
    return keyterms;
  }
}
