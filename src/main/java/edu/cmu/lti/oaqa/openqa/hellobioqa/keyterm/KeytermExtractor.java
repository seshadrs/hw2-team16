package edu.cmu.lti.oaqa.openqa.hellobioqa.keyterm;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.chunk.ConfidenceChunker;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;

/**
 * 
 * @author team16
 * 
 */
public class KeytermExtractor extends AbstractKeytermExtractor {

	private PosTagNamedEntityRecognizer posTagNER;
	private Chunker chunker_token;
	private Chunker chunker_hmm;
	ConfidenceChunker chunker;
	
	/**
	 * Initialize StanfordNLP instance and LingPipe model instance
	 */
	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			//Initialize Standfordnlp Core
			posTagNER = new PosTagNamedEntityRecognizer();
			String hmmpath = this.getClass().getClassLoader().getResource("model/ne-en-bio-genetag.hmmchunker").getPath();
			String tokenpath = this.getClass().getClassLoader().getResource("model/ne-en-bio-genia.TokenShapeChunker").getPath();
			//Instantiate chunkers based on LingPipe model
			chunker_token = (Chunker) AbstractExternalizable.readObject(new File(hmmpath));
			chunker_hmm = (Chunker) AbstractExternalizable.readObject(new File(hmmpath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Judge whether ArrayList "keyterms" contains element "key", if yes, 
	 * return true, otherwise, false
	 * @param keyterms
	 * @param key
	 * @return boolean
	 */
	private boolean containKeyterm(List<Keyterm> keyterms, Keyterm key) {
		Iterator<Keyterm> iter = keyterms.iterator();
		while (iter.hasNext()) {
			Keyterm temp = iter.next();
			if (key.toString().equals(temp.toString()))
				return true;
		}
		return false;

	}
	
	/**
	 * Judge whether string "big" "contains" string "small" while they are
	 * not identical. Here "contains" means "small" is absolutely one token 
	 * of "big", not part of a token
	 * @param big
	 * @param small
	 * @return boolean
	 */
	private boolean containFullString(String big, String small) {
		if (big.contains(small) && !big.equals(small)) {
			int firstindex = big.indexOf(small);
			int afterindex = firstindex + small.length();
			if (firstindex != 0 && big.charAt(firstindex - 1) != ' ') {
				return false;
			}
			if (afterindex != big.length() && big.charAt(afterindex) != ' ') {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}
	/**
	 * Get Keyterms from each question
	 * Keyterm Probability: 0 indicates gene-related, 1 indicates gene-unrelated
	 * 
	 */
	@Override
	protected List<Keyterm> getKeyterms(String question) {
		List<Keyterm> keyterms_hmm = new ArrayList<Keyterm>();
		List<Keyterm> keyterms_nlp_noun = new ArrayList<Keyterm>();
		List<Keyterm> keyterms = new ArrayList<Keyterm>();

		String[] questions = question.split("\\(|\\)");

		// Stanford NLP extracts verbs and nouns spans
		Map<Integer, Integer> verbSpans = posTagNER.getVerbSpans(question);
		Map<Integer, Integer> nounSpans = posTagNER.getNounSpans(question);
		
		// Stanford NLP: Verb section, directly add into Keyterm List
		Set<Entry<Integer, Integer>> entrySet = verbSpans.entrySet();
		for (Entry<Integer, Integer> entry : entrySet) {
			Keyterm verb = new Keyterm(question.substring(entry.getKey(),
					entry.getValue()));
			verb.setProbablity(0);
			keyterms.add(verb);
		}
		// Stanford NLP: Noun section
		entrySet = nounSpans.entrySet();
		for (Entry<Integer, Integer> entry : entrySet) {
			keyterms_nlp_noun.add(new Keyterm(question.substring(
					entry.getKey(), entry.getValue())));
		}

		// LingPipe NER: token_model & hmm_model
		for (int i = 0; i < questions.length; i++) {
			Chunking chunking = chunker_token.chunk(questions[i]);
			for (Chunk c : chunking.chunkSet()) {
				String gene = questions[i].substring(c.start(), c.end());
				keyterms_hmm.add(new Keyterm(gene));
			}
		}
		for (int i = 0; i < questions.length; i++) {
			Chunking chunking = chunker_hmm.chunk(questions[i]);
			for (Chunk c : chunking.chunkSet()) {
				Keyterm gene = new Keyterm(questions[i].substring(c.start(),
						c.end()));
				if (!containKeyterm(keyterms_hmm, gene))
					keyterms_hmm.add(gene);
			}
		}

		// Merge nouns coming from different methods
		// Situation: HMM contains NLP
		for (int i = 0; i < keyterms_hmm.size(); i++) {
			String hmm = keyterms_hmm.get(i).toString();
			for (int j = 0; j < keyterms_nlp_noun.size(); j++) {
				String nlp = keyterms_nlp_noun.get(j).toString();
				//If hmm "full contains" nlp, reserve hmm, discard nlp
				if (containFullString(hmm, nlp)) {
					if (!containKeyterm(keyterms, new Keyterm(hmm))) {
						Keyterm key_hmm = new Keyterm(hmm);
						key_hmm.setProbablity(1);
						keyterms.add(key_hmm);
					}
					keyterms_nlp_noun.remove(j);
					j--;
				}
			}
		}
		// Situation: NLP contains HMM
		for (int i = 0; i < keyterms_nlp_noun.size(); i++) {
			String nlp = keyterms_nlp_noun.get(i).toString();
			for (int j = 0; j < keyterms_hmm.size(); j++) {
				String hmm = keyterms_hmm.get(j).toString();
				if (nlp.contains(hmm)) {
					if (containFullString(nlp, hmm)) {
						//If nlp "full contains" hmm, add nlp, add hmm, and add nlp-hmm
						if (!containKeyterm(keyterms, new Keyterm(nlp))) {
							Keyterm key_nlp = new Keyterm(nlp);
							key_nlp.setProbablity(0);
							keyterms.add(key_nlp);
						}
						if (!containKeyterm(keyterms, new Keyterm(hmm))) {
							Keyterm key_hmm = new Keyterm(hmm);
							key_hmm.setProbablity(1);
							keyterms.add(key_hmm);
						}
						String temp = nlp.replace(hmm, "");
						if (!containKeyterm(keyterms, new Keyterm(temp.trim()))) {
							Keyterm key_temp = new Keyterm(temp.trim());
							key_temp.setProbablity(0);
							keyterms.add(key_temp);
						}
					} else {
						// If nlp = hmm, add nlp
						if (!containKeyterm(keyterms, new Keyterm(nlp))) {
							Keyterm key_hybrid = new Keyterm(nlp);
							key_hybrid.setProbablity(1);
							keyterms.add(key_hybrid);
						}
					}
				}
			}
			if (!containKeyterm(keyterms, new Keyterm(nlp))) {
				// if any hmm does not appear in this specific nlp, add nlp
				Keyterm key_nonrelevant = new Keyterm(nlp);
				key_nonrelevant.setProbablity(0);
				keyterms.add(key_nonrelevant);
			}
		}
		return keyterms;
	}
}
