package edu.cmu.lti.oaqa.openqa.hellobioqa.keyterm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.uima.resource.ResourceInitializationException;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class PosTagNamedEntityRecognizer {

	private StanfordCoreNLP pipeline;

	public PosTagNamedEntityRecognizer() throws ResourceInitializationException {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos");
		pipeline = new StanfordCoreNLP(props);
	}

	public void addToMap(List<CoreLabel> candidate,
			Map<Integer, Integer> begin2end) {
		int begin = candidate.get(0).beginPosition();
		int end = candidate.get(candidate.size() - 1).endPosition();
		begin2end.put(begin, end);
		candidate.clear();
	}

	// Get Verb From Question
	public Map<Integer, Integer> getVerbSpans(String text) {
		Map<Integer, Integer> begin2end = new HashMap<Integer, Integer>();
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			List<CoreLabel> tokenList = sentence.get(TokensAnnotation.class);
			for (int i = 0; i < tokenList.size(); i++) {
				CoreLabel token = tokenList.get(i);
				CoreLabel token_last = null;
				if (i > 0)
					token_last = tokenList.get(i - 1);
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (pos.startsWith("VB")
						&& i > 0
						&& !token_last.get(PartOfSpeechAnnotation.class)
								.equals("WP")
						&& !token_last.get(PartOfSpeechAnnotation.class)
								.equals("WRB") && !pos.equals("VBG")) {
					int begin = token.beginPosition();
					int end = token.endPosition();
					begin2end.put(begin, end);
				}
			}
		}
		return begin2end;
	}

	// Get Noun From Question
	public Map<Integer, Integer> getNounSpans(String text) {
		Map<Integer, Integer> begin2end = new HashMap<Integer, Integer>();
		Annotation document = new Annotation(text);
		pipeline.annotate(document);
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			List<CoreLabel> candidate = new ArrayList<CoreLabel>();
			List<CoreLabel> tokenList = sentence.get(TokensAnnotation.class);
			for (int i = 0; i < tokenList.size(); i++) {
				CoreLabel token = tokenList.get(i);
				CoreLabel token_last = null;
				CoreLabel token_next = null;
				if (i > 0)
					token_last = tokenList.get(i - 1);
				if (i < token.size() - 1)
					token_next = tokenList.get(i + 1);
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (pos.startsWith("JJ") || pos.equals("VBG")) {
					if (candidate.size() > 0) {
						if (i > 0
								&& !token_last
										.get(PartOfSpeechAnnotation.class)
										.startsWith("JJ"))
							addToMap(candidate, begin2end);
					}
					candidate.add(token);
				} else if (pos.startsWith("NN")) {
					if (i > 0
							&& i < tokenList.size() - 1
							&& token_last.get(PartOfSpeechAnnotation.class)
									.equals("DT")
							&& token_next.get(PartOfSpeechAnnotation.class)
									.equals("IN")) {
					} else
						candidate.add(token);
				} else if (candidate.size() > 0) {
					if (pos.equals("POS")) {
						if (sentence
								.toString()
								.substring(token.beginPosition(),
										token.endPosition()).equals("'s")) {
							candidate.add(token);
							continue;
						}
					}
					addToMap(candidate, begin2end);
				}
			}
			if (candidate.size() > 0)
				addToMap(candidate, begin2end);
		}
		return begin2end;
	}
}