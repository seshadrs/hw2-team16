package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval.team16;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.aliasi.chunk.Chunker;
import com.aliasi.util.Streams;

/**
 * 
 * 
 * @author team16
 *
 */
public class GeneSynonymDatabase {
  /**
   * given a gene, return a list of synonym genes related to it (not containt the gene itself)
   * 
   */
  private static HashMap<String, List<String>> hashedDatabase = new HashMap<String, List<String>>();

  /**
   * the static constructor, which used to ensure the txt data only being accessed once 
   * 
   */
  static {
    URL dbURL = GeneSynonymDatabase.class.getClassLoader().getResource("team16/dict/geneDatabase.txt");
    InputStream is = null;
    try {
      is = dbURL.openStream();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    // File databaseFile = new File("team16/dict/geneDatabase.txt");
    try {
      BufferedReader databaseReader = new BufferedReader(new InputStreamReader(is));
      String line = null;
      line = databaseReader.readLine();
      while ((line = databaseReader.readLine()) != null) {
        String[] columns = line.split("\\t");
        String approvedSymbol = columns[1];
        String approvedName = columns[2];
        String previousSymbols = columns[4];
        String previousNames = columns[5];
        String synonyms = columns[6];

        String[] previousSymbolList = previousSymbols.split(",");
        previousNames = previousNames.replaceAll("\"", "");
        String[] previousNameList = previousNames.split(",");
        String[] synonymsList = synonyms.split(",");
        List<String> synList = null;
        // add approvedName as key
        if (hashedDatabase.containsKey(approvedName.trim().toLowerCase())) {
          synList = hashedDatabase.get(approvedName.trim().toLowerCase());
          hashedDatabase.remove(approvedName.trim().toLowerCase());
        } else
          synList = new ArrayList<String>();
        if (!synList.contains(approvedSymbol))
          synList.add(approvedSymbol);

        for (String previousSymbol : previousSymbolList) {
          previousSymbol = previousSymbol.trim();
          if (previousSymbol.equals(""))
            continue;
          if (!synList.contains(previousSymbol))
            synList.add(previousSymbol);
        }
        for (String previousName : previousNameList) {
          previousName = previousName.trim();
          if (previousName.equals(""))
            continue;
          if (!synList.contains(previousName))
            synList.add(previousName);
        }
        for (String synonym : synonymsList) {
          synonym = synonym.trim();
          if (synonym.equals(""))
            continue;
          if (!synList.contains(synonym))
            synList.add(synonym);
        }
        hashedDatabase.put(approvedName.trim().toLowerCase(), synList);

        // add approved symbol as key
        List<String> approvedSymbolSynList = null;
        if (hashedDatabase.containsKey(approvedSymbol.trim().toLowerCase())) {
          approvedSymbolSynList = hashedDatabase.get(approvedSymbol.trim().toLowerCase());
          hashedDatabase.remove(approvedSymbol.trim().toLowerCase());
        } else
          approvedSymbolSynList = new ArrayList<String>();
        for (int i = 0; i < synList.size(); i++) {
          String syn = synList.get(i);
          if (!syn.equals(approvedSymbol) && !approvedSymbolSynList.contains(syn))
            approvedSymbolSynList.add(syn);          
        }
        approvedSymbolSynList.add(approvedName);
        hashedDatabase.put(approvedSymbol.trim().toLowerCase(), approvedSymbolSynList);

        // add previous symbols as key
        for (String previousSymbol : previousSymbolList) {
          previousSymbol = previousSymbol.trim();
          if (previousSymbol.equals(""))
            continue;
          List<String> previousSymbolSynList = null;
          if (hashedDatabase.containsKey(previousSymbol.trim().toLowerCase())) {
            previousSymbolSynList = hashedDatabase.get(previousSymbol.trim().toLowerCase());
            hashedDatabase.remove(previousSymbol.trim().toLowerCase());
          } else
            previousSymbolSynList = new ArrayList<String>();
          for (int i = 0; i < synList.size(); i++) {
            String syn = synList.get(i);
            if (!syn.equals(previousSymbol) && !previousSymbolSynList.contains(syn))
              previousSymbolSynList.add(syn);
          }
          previousSymbolSynList.add(approvedName);
          hashedDatabase.put(previousSymbol.trim().toLowerCase(), previousSymbolSynList);
        }

        // use previous names as key
        for (String previousName : previousNameList) {
          previousName = previousName.trim();
          if (previousName.equals(""))
            continue;
          List<String> previousNameSynList = null;
          if (hashedDatabase.containsKey(previousName.trim().toLowerCase())) {
            previousNameSynList = hashedDatabase.get(previousName.trim().toLowerCase());
            hashedDatabase.remove(previousName.trim().toLowerCase());
          } else
            previousNameSynList = new ArrayList<String>();
          for (int i = 0; i < synList.size(); i++) {
            String syn = synList.get(i);
            if (!syn.equals(previousName) && !previousNameSynList.contains(syn))
              previousNameSynList.add(syn);
          }
          previousNameSynList.add(approvedName);
          hashedDatabase.put(previousName.trim().toLowerCase(), previousNameSynList);
        }

        // use synonyms as key
        for (String synonym : synonymsList) {
          synonym = synonym.trim();
          if (synonym.equals(""))
            continue;
          List<String> synonymSynList = null;
          if (hashedDatabase.containsKey(synonym.trim().toLowerCase())) {
            synonymSynList = hashedDatabase.get(synonym.trim().toLowerCase());
            hashedDatabase.remove(synonym.trim().toLowerCase());
          } else
            synonymSynList = new ArrayList<String>();
          for (int i = 0; i < synList.size(); i++) {
            String syn = synList.get(i);
            if (!syn.equals(synonym) && !synonymSynList.contains(syn))
              synonymSynList.add(syn);
          }
          synonymSynList.add(approvedName);
          hashedDatabase.put(synonym.trim().toLowerCase(), synonymSynList);
        }
      }
      databaseReader.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  /**
   * searchGeneSynonyms provide you all the gene synonyms given a gene
   * 
   * @param gene
   * @return
   */
  public static List<String> searchGeneSynonyms(String gene){
    List<String> result = hashedDatabase.get(gene);
    return result;
  }
}