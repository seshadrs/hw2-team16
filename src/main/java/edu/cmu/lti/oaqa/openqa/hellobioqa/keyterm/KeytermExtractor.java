/*
 *  Copyright 2012 Carnegie Mellon University
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package edu.cmu.lti.oaqa.openqa.hellobioqa.keyterm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

public class KeytermExtractor extends AbstractKeytermExtractor {

	private PosTagNamedEntityRecognizer posTagNER;
	private Chunker chunker_token;
	private Chunker chunker_hmm;
	ConfidenceChunker chunker;

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			posTagNER = new PosTagNamedEntityRecognizer();
			chunker_token = (Chunker) AbstractExternalizable
					.readObject(new File(
							"src/main/resources/model/ne-en-bio-genia.TokenShapeChunker"));
			chunker_hmm = (Chunker) AbstractExternalizable.readObject(new File(
					"src/main/resources/model/ne-en-bio-genetag.hmmchunker"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean containKeyterm(List<Keyterm> keyterms, Keyterm key) {
		Iterator<Keyterm> iter = keyterms.iterator();
		while (iter.hasNext()) {
			Keyterm temp = iter.next();
			if (key.toString().equals(temp.toString()))
				return true;
		}
		return false;

	}

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

	@Override
	protected List<Keyterm> getKeyterms(String question) {
		List<Keyterm> keyterms_hmm = new ArrayList<Keyterm>();
		List<Keyterm> keyterms_nlp_noun = new ArrayList<Keyterm>();
		List<Keyterm> keyterms = new ArrayList<Keyterm>();

		String[] questions = question.split("\\(|\\)");

		// Stanford NLP extracts verbs and general nouns
		Map<Integer, Integer> verbSpans = posTagNER.getVerbSpans(question);
		Map<Integer, Integer> nounSpans = posTagNER.getNounSpans(question);
		// Verb section
		Set<Entry<Integer, Integer>> entrySet = verbSpans.entrySet();
		for (Entry<Integer, Integer> entry : entrySet) {
			Keyterm verb = new Keyterm(question.substring(entry.getKey(),
					entry.getValue()));
			verb.setProbablity(0);
			keyterms.add(verb);
		}
		// Noun section
		entrySet = nounSpans.entrySet();
		for (Entry<Integer, Integer> entry : entrySet) {
			keyterms_nlp_noun.add(new Keyterm(question.substring(
					entry.getKey(), entry.getValue())));
		}

		// LingPipe NER
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

		// Noun Merge
		// HMM contains NLP
		for (int i = 0; i < keyterms_hmm.size(); i++) {
			String hmm = keyterms_hmm.get(i).toString();
			for (int j = 0; j < keyterms_nlp_noun.size(); j++) {
				String nlp = keyterms_nlp_noun.get(j).toString();
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
		// NLP contains HMM
		for (int i = 0; i < keyterms_nlp_noun.size(); i++) {
			String nlp = keyterms_nlp_noun.get(i).toString();
			for (int j = 0; j < keyterms_hmm.size(); j++) {
				String hmm = keyterms_hmm.get(j).toString();
				if (nlp.contains(hmm)) {
					if (containFullString(nlp, hmm)) {
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
						// Identical
						if (!containKeyterm(keyterms, new Keyterm(nlp))) {
							Keyterm key_hybrid = new Keyterm(nlp);
							key_hybrid.setProbablity(1);
							keyterms.add(key_hybrid);
						}
					}
				}
			}
			if (!containKeyterm(keyterms, new Keyterm(nlp))) {
				// non-relevant
				Keyterm key_nonrelevant = new Keyterm(nlp);
				key_nonrelevant.setProbablity(0);
				keyterms.add(key_nonrelevant);
			}
		}

		// compare with gs
		try {
			// Create file
			FileWriter fstream = new FileWriter("out.txt", true);
			BufferedWriter out = new BufferedWriter(fstream);

			Iterator<Keyterm> iter = keyterms.iterator();
			while (iter.hasNext()) {
				String keyterm = iter.next().toString();
				out.append(keyterm + ", ");
			}
			out.append("\n");
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		return keyterms;
	}
}
