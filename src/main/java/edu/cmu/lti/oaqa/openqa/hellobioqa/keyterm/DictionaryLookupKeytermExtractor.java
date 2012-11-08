package edu.cmu.lti.oaqa.openqa.hellobioqa.keyterm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;

public class DictionaryLookupKeytermExtractor extends AbstractKeytermExtractor{
  private BufferedReader geneDataReader;
  private ArrayList<String> geneList;
  private PosTagNamedEntityRecognizer nounRecognizer;
  
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    File geneData=new File("src/main/resources/model/dictionary/genenames.txt");
    geneList=new ArrayList<String>();
    
    try {
      geneDataReader=new BufferedReader(new FileReader(geneData));
      String geneName;
      while((geneName=geneDataReader.readLine())!=null){
        geneList.add(geneName.trim().toUpperCase());
      }
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      nounRecognizer=new PosTagNamedEntityRecognizer();
    } catch (ResourceInitializationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  @Override
  protected List<Keyterm> getKeyterms(String context) {
    // TODO Auto-generated method stub
    List<Keyterm> keyterms = new ArrayList<Keyterm>();
    Map<Integer,Integer> map =nounRecognizer.getGeneSpans(context);
    Iterator beginIterator=map.keySet().iterator();
    while(beginIterator.hasNext()){
      Integer begin=(Integer)beginIterator.next();
      Integer end=map.get(begin);
      String noun = context.substring(begin, end);
      if(geneList.contains(noun.trim().toUpperCase())){
        keyterms.add(new Keyterm(noun));
      }
    }
    return keyterms;
  }
}
