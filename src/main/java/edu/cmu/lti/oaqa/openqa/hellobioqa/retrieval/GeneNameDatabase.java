package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;


public class GeneNameDatabase {
  public static HashMap<String,Gene> hashedDatabase = new HashMap<String,Gene>();
  public static HashMap<String,Gene> synonymDatabase = new HashMap<String,Gene>();
  static {
    File databaseFile = new File("geneDatabase.txt");
    try {
      BufferedReader databaseReader = new BufferedReader(new FileReader(databaseFile));
      String line = null;
      line = databaseReader.readLine();
      while((line = databaseReader.readLine()) != null){
        String[] columns = line.split("\\t");
        int id = Integer.parseInt(columns[0].substring(5));
        String approvedSymbol = columns[1];
        String approvedName = columns[2];
        String status = columns[3];
        String previousNames = columns[4];
        String synonyms = columns[5];
        String chromosome = columns[6];
        Gene gene = new Gene(id);
        gene.setApprovedSymbol(approvedSymbol);
        gene.setApprovedName(approvedName);
        gene.setStatus(status);
        gene.setPreviousNames(previousNames);
        gene.setSynonyms(synonyms);
        gene.setChromosome(chromosome);
        hashedDatabase.put(approvedName, gene);
        synonyms = synonyms.replaceAll("\"", "");
        String[] synonymList = synonyms.split(", ");
        for (String synonym : synonymList){
          if (synonym.equals(""))
            continue;
          if (!synonymDatabase.containsKey(synonym))
            synonymDatabase.put(synonym, gene);
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
  
  public static Gene getGene(String name){
    if (hashedDatabase.containsKey(name))
      return hashedDatabase.get(name);
    if (synonymDatabase.containsKey(name))
      return synonymDatabase.get(name);
    return null;
  }
}
