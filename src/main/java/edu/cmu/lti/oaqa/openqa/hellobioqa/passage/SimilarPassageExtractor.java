package edu.cmu.lti.oaqa.openqa.hellobioqa.passage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.jsoup.Jsoup;

import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.PassageCandidate;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;

import edu.cmu.lti.oaqa.openqa.hello.passage.SimplePassageExtractor;

import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.aliasi.spell.TfIdfDistance;

public class SimilarPassageExtractor extends SimplePassageExtractor {

  // the number of sentences a passage is composed of
  private int sentencesPerPassage = 3;

  // the fraction of the top ranking passages that are selected as the result
  private double topPassagesFraction = 0.75;

  // tfidf distance object
  private TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;

  private TfIdfDistance tfIdf = new TfIdfDistance(tokenizerFactory);

  private double findQuestionPassageSimilarity(String question, String passage) {
    double similarity = tfIdf.proximity(question, passage);
    ;
    // System.out.println("SIMILARITY:\t"+similarity);
    return similarity;
  }

  private ArrayList<PassageCandidate> getPassageCandidateSpans(String documentText,
          String documentID, String question) throws AnalysisEngineProcessException {
    ArrayList<PassageCandidate> sentenceSpans = new ArrayList<PassageCandidate>();
    ArrayList<PassageCandidate> passageSpans = new ArrayList<PassageCandidate>();

    final TokenizerFactory TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
    final SentenceModel SENTENCE_MODEL = new MedlineSentenceModel();

    List<String> tokenList = new ArrayList<String>();
    List<String> whiteList = new ArrayList<String>();
    Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(documentText.toCharArray(), 0,
            documentText.length());
    tokenizer.tokenize(tokenList, whiteList);

    String[] tokens = new String[tokenList.size()];
    String[] whites = new String[whiteList.size()];
    tokenList.toArray(tokens);
    whiteList.toArray(whites);
    int[] sentenceBoundaries = SENTENCE_MODEL.boundaryIndices(tokens, whites);

    int sentStartTok = 0;
    int sentEndTok = 0;
    int sentStartIndex = 0;
    int sentEndIndex = 0;

    for (int i = 0; i < sentenceBoundaries.length; ++i) {
      sentEndTok = sentenceBoundaries[i];
      // System.out.println("SENTENCE "+(i+1)+": ");
      for (int j = sentStartTok; j <= sentEndTok; j++) {
        // System.out.print(tokens[j]+whites[j+1]);
        sentEndIndex += tokens[j].length() + whites[j + 1].length();
      }

      // System.out.println();
      // System.out.println("@@@@@@@"+documentText.substring(sentStartIndex,sentEndIndex));

      sentenceSpans.add(new PassageCandidate(documentID, sentStartIndex, sentEndIndex, (float) 0.0,
              question));

      sentStartTok = sentEndTok + 1;
      sentStartIndex = sentEndIndex;
    }

    for (int i = 0; i < sentenceSpans.size() - (sentencesPerPassage - 1); i += (sentencesPerPassage - 1)) {
      PassageCandidate passageStartingSentence = sentenceSpans.get(i);
      PassageCandidate passageEndingSentence = sentenceSpans.get(i + (sentencesPerPassage - 1));
      passageSpans.add(new PassageCandidate(documentID, passageStartingSentence.getStart(),
              passageEndingSentence.getEnd(), (float) 0.0, question));
    }

    return passageSpans;
  }

  private List<PassageCandidate> setPassageCandidateScores(List<PassageCandidate> passages,
          String question, String documentText) {
    // remove question words from the question so that TFIDF doesn't go crazy thinking those words
    // are important
    String cleanedQuestion = question;
    ArrayList<String> questionWords = new ArrayList<String>();
    questionWords.add("Which");
    questionWords.add("What");
    questionWords.add("How");
    questionWords.add("which");
    questionWords.add("what");
    questionWords.add("how");
    for (String qw : questionWords) {
      cleanedQuestion = cleanedQuestion.replace(qw, "");
    }

    for (int i = 0; i < passages.size(); i++) {
      PassageCandidate passage = passages.get(i);
      double similarityScore = findQuestionPassageSimilarity(question,
              documentText.substring(passage.getStart(), passage.getEnd()));
      passages.get(i).setProbablity((float) similarityScore);
    }

    return passages;
  }

  private List<PassageCandidate> sortPassageCandidates(List<PassageCandidate> passages) {
    Collections.sort(passages, new PassageCandidateComparator());
    return passages;
  }

  private List<PassageCandidate> filterPassageCandidates(List<PassageCandidate> passages) {
    int totalCandidates = (int) ((double) passages.size() * topPassagesFraction);
    return passages.subList(0, totalCandidates);
  }
  
  
  private String getDocumentText(String ID)
  {
    String htmlText;
    try {
      htmlText = wrapper.getDocText(ID);
      String documentText = Jsoup.parse(htmlText).text().replaceAll("([\177-\377\0-\32]*)", "");
      return documentText;
    } catch (SolrServerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
    return "";
  }
  
  private String getDocumentHtml(String ID)
  {
    String htmlText;
    try {
      htmlText = wrapper.getDocText(ID);
      return htmlText;
    } catch (SolrServerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
    return "";
  }
  
  private List<PassageCandidate> reverseMapPassageSpans( List<PassageCandidate> allPassages)
  {
    for(PassageCandidate passage : allPassages)
    {
      String documentText = getDocumentText(passage.getDocID());
      String documentHtml = getDocumentHtml(passage.getDocID());
      String passageText = documentText.substring(passage.getStart(),passage.getEnd());
      
      String[] words = passageText.split(" ");
      int sentenceWordCount = words.length;
      
      if (documentHtml.contains(words[0]))
      {
        int sentenceStart=documentHtml.indexOf(words[0]);
        int sentenceEnd=sentenceStart + documentHtml.substring(sentenceStart).indexOf(words[sentenceWordCount-1]);
        if (sentenceEnd==sentenceStart-1)
          sentenceEnd= documentHtml.length();
        passage.setStart(sentenceStart);
        passage.setEnd(sentenceEnd);
      }
      else
      {
        //System.out.println("!!!!!!!!!!!!!!!!!!!!!!OOPS!!");
      }
       
    }
    
    return allPassages;
  }

  @Override
  protected List<PassageCandidate> extractPassages(String question, List<Keyterm> keyterms,
          List<RetrievalResult> documents) {

    List<PassageCandidate> result = new ArrayList<PassageCandidate>();
    List<PassageCandidate> allPassages = new ArrayList<PassageCandidate>();
    List<PassageCandidate> passages = new ArrayList<PassageCandidate>();
    

    // compute the tfidf tables using text from all documents
    for (RetrievalResult document : documents) {

      try {
        String id = document.getDocID();
        String htmlText = wrapper.getDocText(id);
        // cleaning HTML text
        String documentText = Jsoup.parse(htmlText).text().replaceAll("([\177-\377\0-\32]*)", "");
        // TODO .trim() ?
        // // for now, making sure the text isn't too long
        // String documentText = text.substring(0, Math.min(5000, text.length()));

        tfIdf.handle(documentText);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // //print tfidf table
    // System.out.printf("\n  %18s  %8s  %8s\n","Term", "Doc Freq", "IDF");
    // for (String term : tfIdf.termSet())
    // System.out.printf("  %18s  %8d  %8.2f\n",term,tfIdf.docFrequency(term),tfIdf.idf(term));

    // extract all passages in a document
    for (RetrievalResult document : documents) {
      //System.out.println("RetrievalResult: " + document.toString());
      String id = document.getDocID();
      try {
        String htmlText = wrapper.getDocText(id);

        // cleaning HTML text
        String text = Jsoup.parse(htmlText).text().replaceAll("([\177-\377\0-\32]*)", "")/* .trim() */;
        // for now, making sure the text isn't too long
        text = text.substring(0, Math.min(5000, text.length()));
        //System.out.println(text);

        // get the list of all passages
        passages = getPassageCandidateSpans(text, id, question);

        // set scores for each passage based on its similarity with the question
        passages = setPassageCandidateScores(passages, question, text);

        // add passages from this document to all pasasges list
        for (int i = 0; i < passages.size(); i++)
          allPassages.add(passages.get(i));

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // sort all passages by their scores
    allPassages = sortPassageCandidates(allPassages);

    // retrieve the top ranking passages
    allPassages= filterPassageCandidates(allPassages);

    for (int i = 0; i < result.size(); i++)
      System.out.println("PROB:" + allPassages.get(i).getProbability());
    
    result = reverseMapPassageSpans(allPassages);
    
    return result;
  }

  private class PassageCandidateComparator implements Comparator {
    // Ranks by score, decreasing.
    public int compare(Object o1, Object o2) {
      PassageCandidate s1 = (PassageCandidate) o1;
      PassageCandidate s2 = (PassageCandidate) o2;
      if (s1.getProbability() < s2.getProbability()) {
        return 1;
      } else if (s1.getProbability() > s2.getProbability()) {
        return -1;
      }
      return 0;
    }
  }

}