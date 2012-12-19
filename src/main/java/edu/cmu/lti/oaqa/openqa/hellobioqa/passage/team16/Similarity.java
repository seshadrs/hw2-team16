package edu.cmu.lti.oaqa.openqa.hellobioqa.passage.team16;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.aliasi.spell.TfIdfDistance;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class Similarity {

  public static class NgramSimilarity {

    public static String[] ngramsInText(String text, int ngramOrder) {
      /*
       * Returns the ngrams(Strings) as an array for a given string and the ngram order.
       */
      String[] tokens = text.split(" ");
      if (tokens.length < ngramOrder) {
        String[] result = {};
        return result;
      }

      String[] result = new String[tokens.length - ngramOrder + 1];
      for (int i = 0; i < tokens.length - ngramOrder + 1; i++) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < ngramOrder; k++) {
          if (k > 0)
            sb.append(' ');
          sb.append(tokens[i + k]);
        }
        result[i] = sb.toString();
      }
      return result;
    }

    public static int ngramsOverlap(String[] ngrams1, String[] ngrams2) {
      /*
       * Returns the number of ngrams that overlap between the two ngram arrays (Strings[])
       */
      int overlap = 0;
      if (ngrams1.length == 0 || ngrams2.length == 0)
        return 0;

      for (int i = 0; i < ngrams1.length; i++)
        for (int j = 0; j < ngrams2.length; j++) {
          if (ngrams1[i].compareTo(ngrams2[j]) == 0) // if the ngrams are the same
          {
            // System.out.println("$$$$$\tMATCH");
            overlap += 1;
          } else {
            // System.out.println("<<<<<\tNO MATCH\t");
          }
        }

      return overlap;
    }

    public static double questionPassageSimilarity(String question, String passage,
            int maxNgramOrder) {
      int matches = 0;
      if (question == "" || passage == "")
        return 0;

      // consider lower case form of question and passage
      question = question.toLowerCase();
      passage = passage.toLowerCase();

      for (int i = 1; i <= maxNgramOrder; i++) {
        String[] questionNgrams = ngramsInText(question, i);
        String[] passageNgrams = ngramsInText(passage, i);
        // System.out.println("#####\t"+i);

        matches += ngramsOverlap(questionNgrams, passageNgrams);

      }

      return (double) matches;
    }

  }

  public static class TFIDFSimilarity {

    // tfidf distance object
    private static TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;

    private static TfIdfDistance tfIdf = new TfIdfDistance(tokenizerFactory);

    private static boolean trained = false;

    public static void trainModel(List<String> documents) {
      // compute the TFIDF tables using text from all documents (raw text)
      for (String document : documents) {

        try {
          tfIdf.handle(document);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

    }

    public static double questionPassageSimilarity(String question, String passage) {
      double similarity = tfIdf.proximity(question, passage);
      // System.out.println("SIMILARITY:\t"+similarity);
      return similarity;
    }

  }

  public static class SynonymsSimilarity {

    private static String getSynonyms(String originalWord, int synCount) {
      // System.out.println("@@@\tFUnc Called");
      URL url = null;
      // List<String> synList = new ArrayList<String>();
      String synList = "";
      IDictionary dict = null;
      int count = 0;
      try {
        url = new URL("file", null, "dict");
        dict = new Dictionary(url);
        dict.open();

        IIndexWord idxWord = dict.getIndexWord(originalWord, POS.NOUN);
        // Find the first meaning of the word
        if (idxWord != null) {
          IWordID wordID = idxWord.getWordIDs().get(0);
          IWord word = dict.getWord(wordID);
          ISynset synset = word.getSynset();
          for (IWord w : synset.getWords()) {
            // replace "_" with ""
            String synonym = w.getLemma().replaceAll("_", " ");
            // synList.add(synonym);
            synList += synonym + " ";
            count++;
            if (count == synCount)
              break;
          }
        } else {
          return null;
        }

      } catch (Exception e) {
        // e.printStackTrace();
      }
      return synList;
    }

    public static List<String> keytermsSynonymsList(String keytermsText) {
      // System.out.println("********\t"+keytermsText);
      List<String> keytermsSynonyms = new ArrayList();
      for (String keyterm : keytermsText.split(" ")) {
        // System.out.println("\t********\t"+keyterm);
        String keytermSyns = getSynonyms(keyterm, 4);
        if (keytermSyns != null)
          keytermsSynonyms.add(keytermSyns.toLowerCase());
      }
      return keytermsSynonyms;
    }

    public static double questionPassageSimilarity(String question, String passage,
            List<String> keytermsSynonyms) {
      double similarity = 1.0;
      passage = passage.toLowerCase();

      for (String keytermSynonymsText : keytermsSynonyms) {
        int matchCount = 0;
        String[] keytermSynonyms = keytermSynonymsText.split(" ");
        for (String synonym : keytermSynonyms) {
          if (passage.contains(synonym))
            matchCount += 1;
        }
        similarity *= (0.001 + matchCount);

      }

      // System.out.println("Syn SIm !!!!\t"+similarity);
      return similarity;

    }

  }

}
