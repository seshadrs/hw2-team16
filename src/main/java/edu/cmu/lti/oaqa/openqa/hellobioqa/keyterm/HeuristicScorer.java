package edu.cmu.lti.oaqa.openqa.hellobioqa.keyterm;


public class HeuristicScorer  {
	/*
	 * Uses heuristic functions to score the gene candidates.
	 * Functions are based on string length, string patterns that resemble genes.
	 * Updates the score of the gene candidates in the CAS. 
	 */
	
	
	
	private static double score_Length(String candidate)
	{
		return ((float)candidate.length())*((float)candidate.length())/500.00;
	}
	
	
	private static double score_StringPattern( String candidate)
	{
		if (candidate.length()<=1)
			return 0.0;
		
		char first_character = candidate.charAt(0);
		char second_character = candidate.charAt(1);
		
		double local_score = 0.0;
		
		
		if ( first_character >='a' && first_character <='z')
		{
			if (second_character >= '0' && second_character <= '9')
				local_score += 1.0;
		}
		
		if ((first_character >='A' && first_character <='Z') || first_character >= '0' && first_character <= '9')	//likely a proper noun
			local_score += 0.5;
		
		if ((first_character =='(' || first_character =='[') && (second_character >='A' && second_character <='Z' ))	//likely a technical proper noun/acronym within brackets 
			local_score += 0.5;
		
		if ((first_character >='A' && first_character <='Z') && (second_character >='A' && second_character <='Z' ))	//likely an acronym
			local_score += 0.5;
		
		return local_score;
	}
	
	

	
	public static double getScore(String candidate) {
		// TODO Auto-generated method stub
		
System.out.println("**In processCas()");
		
		
	    try {
	  	
	        	double score=0.0;
	        	
	        	score+=score_Length(candidate);
	        	score+=score_StringPattern(candidate);
	        	
	        	return score;
	          	          
	        
	    	
	      } catch (Exception e) {
	    	  	e.printStackTrace();
	    	  	return 0.0;
	      }
	    
		
	}

}
