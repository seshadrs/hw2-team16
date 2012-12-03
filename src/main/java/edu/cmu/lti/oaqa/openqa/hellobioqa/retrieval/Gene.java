package edu.cmu.lti.oaqa.openqa.hellobioqa.retrieval;

public class Gene {
  private int hgncId;
  private String approvedSymbol;
  private String approvedName;
  private String status;
  private String previousNames;
  private String synonyms;
  private String chromosome;
  
  public Gene(int hgncId){
    this.hgncId = hgncId;
  }
  
  public void setApprovedSymbol(String approvedSymbol){
    this.approvedSymbol = approvedSymbol;
  }
  
  public void setApprovedName(String approvedName){
    this.approvedName = approvedName;
  }
  
  public void setStatus(String status){
    this.status = status;
  }
  
  public void setPreviousNames(String previousNames){
    this.previousNames = previousNames;
  }
  
  public void setSynonyms(String synonyms){
    this.synonyms = synonyms;
  }
  
  public void setChromosome(String chromosome){
    this.chromosome = chromosome;
  }
  
  public int getHgncId(){
    return this.hgncId;
  }
  
  public String getApprovedSymbol(){
    return this.approvedSymbol;
  }
  
  public String getApprovedName(){
    return this.approvedName;
  }
  
  public String getStatus(){
    return this.status;
  }
  
  public String getPreviousNames(){
    return this.previousNames;
  }
  
  public String getSynonyms(){
    return this.synonyms;
  }
  
  public String getChromosome(){
    return this.chromosome;
  }
}
