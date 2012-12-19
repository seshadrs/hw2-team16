package edu.cmu.lti.oaqa.openqa.hellobioqa.passage.team16;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.jsoup.Jsoup;

import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.PassageCandidate;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;

import edu.cmu.lti.oaqa.openqa.hello.passage.SimplePassageExtractor;
import edu.cmu.lti.oaqa.openqa.hellobioqa.passage.team16.Similarity.NgramSimilarity;
import edu.cmu.lti.oaqa.openqa.hellobioqa.passage.team16.Similarity.SynonymsSimilarity;
import edu.cmu.lti.oaqa.openqa.hellobioqa.passage.team16.Similarity.TFIDFSimilarity;

import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

public class SimilarBioPassageExtractor extends SimplePassageExtractor {

  // the number of sentences a passage is composed of
  private int sentencesPerPassage = 3;

  // the fraction of the top ranking passages that are selected as the result
  private double topPassagesFraction = 0.375;

  private int[] mapPassageInHtml(String passageText, String documentHtml, int defaultStart,
          int defaultEnd) {

    String[] words = passageText.split(" ");
    int sentenceWordCount = words.length;

    int passageStart = defaultStart;
    int passageEnd = defaultEnd;
    if (documentHtml.contains(words[0])) {
      passageStart = documentHtml.indexOf(words[0]);
      passageEnd = passageStart
              + documentHtml.substring(passageStart).indexOf(words[sentenceWordCount - 1]);
      if (passageEnd == passageStart - 1)
        passageEnd = documentHtml.length();
    } else {
      // System.out.println("!!!!!!!!!!!!!!!!!!!!!!OOPS!!");
    }

    int[] span = { passageStart, passageEnd };
    return span;
  }

  private List<Integer> occuranceIndices(String substring, String text) {
    // System.out.println("!!!\tOI");
    List<Integer> indices = new ArrayList();
    if (substring == "" || text.contains(substring) == false)
      return indices;
    int ctr = 0;
    while (text.contains(substring)) {
      // System.out.println("OI");
      try {
        indices.add(text.indexOf(substring));
        text = text.replaceFirst(substring, "");
      } catch (Exception e) {
      }
      ctr++;
      if (ctr > 10)
        break;
    }

    return indices;
  }

  private int[] mapPassageInHtmlExhaustive(String passageText, String documentHtml,
          int defaultStart, int defaultEnd) {
    // System.out.print("!!!\tEntered\t");
    passageText = passageText.toLowerCase();
    documentHtml = documentHtml.toLowerCase();

    String[] words = passageText.split(" ");

    int passageStart = defaultStart;
    int passageEnd = defaultEnd;

    List<Integer> possibleStartIndices = new ArrayList();
    int i = 0;
    int ctr = 0;
    while (i < words.length && possibleStartIndices.size() == 0) {
      possibleStartIndices = occuranceIndices(words[i], documentHtml);
      // System.out.println("WL1");
      i += 1;
      ctr++;
      if (ctr > 10)
        break;
    }
    List<Integer> possibleEndIndices = new ArrayList();
    int j = words.length - 1;
    ctr = 0;
    while (j > 0 && possibleEndIndices.size() == 0) {
      possibleEndIndices = occuranceIndices(words[j], documentHtml);
      // System.out.println("WL2");
      j -= 1;
      ctr++;
      if (ctr > 10)
        break;
    }

    Collections.reverse(possibleEndIndices);
    // System.out.println("EX WL2");
    double bestScore = 0.00;
    for (Integer possibleStartIndex : possibleStartIndices.subList(0,
            Math.min(possibleStartIndices.size(), 10))) {
      for (Integer possibleEndIndex : possibleEndIndices.subList(0,
              Math.min(possibleEndIndices.size(), 10))) {
        if (possibleEndIndex <= possibleStartIndex)
          break;

        String text = documentHtml.substring(possibleStartIndex, possibleEndIndex);
        Integer length = possibleEndIndex - possibleStartIndex;
        Integer wordMatches = 0;

        for (String w : words) {
          if (text.contains(w))
            wordMatches += 1;
        }

        double score = (double) wordMatches / (double) length;
        if (score >= bestScore) {
          bestScore = score;
          passageStart = possibleStartIndex;
          passageEnd = possibleEndIndex;
        }
      }
    }

    int[] span = { passageStart, passageEnd };
    // System.out.println("Done");
    return span;
  }

  public static int nthOccurrence(String str, char c, int n) {
    int pos = str.indexOf(c, 0);
    while (n-- > 0 && pos != -1)
      pos = str.indexOf(c, pos + 1);
    return pos;
  }

  private ArrayList<PassageCandidate> getPassageCandidateSpans(String documentText,
          String documentID, String question, String documentHtml)
          throws AnalysisEngineProcessException {
    /*
     * Extracts all the passage candidates from the given document (in raw text) !! The returned
     * passage spans have hte passage text in the question field. It needs to be replaced with the
     * question in the very last step.
     */

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

    // find sentence start and end boundaries in the document, add the sentences to sentence spans
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
              ""));

      sentStartTok = sentEndTok + 1;
      sentStartIndex = sentEndIndex;
    }

    for (int i = 0; i < sentenceSpans.size() - (sentencesPerPassage - 1); i += (sentencesPerPassage - 1)) {
      PassageCandidate passageStartingSentence = sentenceSpans.get(i);
      PassageCandidate passageEndingSentence = sentenceSpans.get(i + (sentencesPerPassage - 1));

      String passageText = documentText.substring(passageStartingSentence.getStart(),
              passageStartingSentence.getEnd());
      int defaultStart = passageStartingSentence.getStart();
      int defaultEnd = passageEndingSentence.getEnd();
      int[] passageHtmlSpan = mapPassageInHtml(passageText, documentHtml, defaultStart, defaultEnd);
      passageSpans.add(new PassageCandidate(documentID, passageHtmlSpan[0], passageHtmlSpan[1],
              (float) 0.0, passageText));
    }

    return passageSpans;
  }

  private List<PassageCandidate> setPassageCandidateScores(List<PassageCandidate> passages,
          String question, String keytermsText, List<String> keytermsSynonyms) {

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

    String synonymsText = "";
    for (String synList : keytermsSynonyms)
      synonymsText += synList + " ";
    synonymsText = synonymsText.trim().replace("  ", " ");

    // if the question has a '?' character, seperate from text so that it gets tokenized separately.
    if (cleanedQuestion.contains("?"))
      cleanedQuestion.replace("?", " ?");

    for (int i = 0; i < passages.size(); i++) {
      PassageCandidate passage = passages.get(i);
      double TFIDFSimilarityScore = TFIDFSimilarity.questionPassageSimilarity(question,
              passage.getQueryString());
      double NgramSimilarityScore = NgramSimilarity.questionPassageSimilarity(
              question.substring(nthOccurrence(question, ' ', 2) + 1), passage.getQueryString(), 3);
      double KeytermSimilarityScore = NgramSimilarity.questionPassageSimilarity(keytermsText,
              passage.getQueryString(), 2);
      double SynonymSimilarity = SynonymsSimilarity.questionPassageSimilarity(question,
              passage.getQueryString(), keytermsSynonyms);
      double synonymExtendedTFIDFSimilarityScore = TFIDFSimilarity.questionPassageSimilarity(
              question + " " + synonymsText, passage.getQueryString());
      double similarityScore = (double) (0.0001 + KeytermSimilarityScore)
              * (0.00000001 + TFIDFSimilarityScore) * (0.0001 + SynonymSimilarity)
              * (0.00000001 + synonymExtendedTFIDFSimilarityScore);
      // double similarityScore = (double)
      // Math.exp((double)(KeytermSimilarityScore+TFIDFSimilarityScore+SynonymSimilarity));
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

  @Override
  protected List<PassageCandidate> extractPassages(String question, List<Keyterm> keyterms,
          List<RetrievalResult> documents) {

    List<PassageCandidate> result = new ArrayList<PassageCandidate>();
    List<PassageCandidate> allPassages = new ArrayList<PassageCandidate>();
    List<PassageCandidate> passages = new ArrayList<PassageCandidate>();

    String keytermsText = "";
    for (Keyterm keyterm : keyterms)
      keytermsText += keyterm.getText() + " ";
    keytermsText = keytermsText.trim();
    List<String> keytermsSynonyms = SynonymsSimilarity.keytermsSynonymsList(keytermsText);

    // compute the TFIDF tables using text from all documents
    List<String> allDocuments = new ArrayList();
    for (RetrievalResult document : documents) {

      try {
        String id = document.getDocID();
        String htmlText = wrapper.getDocText(id);
        // cleaning HTML text
        String documentText = Jsoup.parse(htmlText).text().replaceAll("([\177-\377\0-\32]*)", "");
        allDocuments.add(documentText);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    TFIDFSimilarity.trainModel(allDocuments);

    // //print tfidf table
    // System.out.printf("\n  %18s  %8s  %8s\n","Term", "Doc Freq", "IDF");
    // for (String term : tfIdf.termSet())
    // System.out.printf("  %18s  %8d  %8.2f\n",term,tfIdf.docFrequency(term),tfIdf.idf(term));

    // extract all passages in a document
    for (RetrievalResult document : documents) {
      System.out.println("RetrievalResult: " + document.toString());
      String id = document.getDocID();
      try {
        String htmlText = wrapper.getDocText(id);

        // cleaning HTML text
        String text = Jsoup.parse(htmlText).text().replaceAll("([\177-\377\0-\32]*)", "")/* .trim() */;
        // Making sure the text isn't too long to print
        String printText = text.substring(0, Math.min(500, text.length()));
        System.out.println(printText);

        // get the list of all passages
        passages = getPassageCandidateSpans(text, id, question, htmlText);

        passages = setPassageCandidateScores(passages, question, keytermsText, keytermsSynonyms);

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
    result = filterPassageCandidates(allPassages);
    // int resultSize = 90;
    // if (allPassages.size() > resultSize)
    // result = allPassages.subList(0, resultSize);

    // for (int i = 0; i < result.size(); i++)
    // System.out.println("PROB:" + result.get(i).getProbability());

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