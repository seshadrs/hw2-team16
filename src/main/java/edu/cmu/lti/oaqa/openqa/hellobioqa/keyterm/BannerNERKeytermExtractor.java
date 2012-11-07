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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

import banner.BannerProperties;
import banner.Sentence;
import banner.tagging.CRFTagger;
import banner.tagging.Mention;
import banner.tokenization.Tokenizer;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;

/**
 * 
 * @author Mingtao Zhang <mingtaoz@cmu.edu>
 * 
 */
public class BannerNERKeytermExtractor extends AbstractKeytermExtractor {

  private Chunker chunker;

  private String propertiesFilename;// banner.properties

  private String modelFilename; // model.bin

  private BannerProperties properties;

  private Tokenizer tokenizer;

  private CRFTagger tagger;

  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    propertiesFilename = "src/main/resources/model/banner.properties";// (String)
                                                                      // aContext.getConfigParameterValue("propertiesFilename");
    modelFilename = "src/main/resources/model/gene_model_v02.bin";// (String)
                                                                  // aContext.getConfigParameterValue("modelFilename");
    try {
      properties = BannerProperties.load(propertiesFilename);
      tokenizer = properties.getTokenizer();
      tagger = CRFTagger.load(new File(modelFilename), properties.getLemmatiser(),
              properties.getPosTagger());
      // chunker = (Chunker) AbstractExternalizable.readObject(new File(featurePath));
    } catch (Exception e) {
      // don't case about different types of Exceptions
      e.printStackTrace();
    }

  }

  @Override
  protected List<Keyterm> getKeyterms(String question) {
    List<Keyterm> keyterms = new ArrayList<Keyterm>();
    Sentence bsentence = new Sentence(null, question);
    tokenizer.tokenize(bsentence);
    tagger.tag(bsentence);
    String previous = "abc";
    for (Mention m : bsentence.getMentions()) {
      if (m.toString().contains("GENE") && !m.toString().contains(previous)) {
        String rawGene = m.toString();// sentence.substring(t.getToken().getStart(),
                                      // t.getToken().getEnd());
        String gene = rawGene.substring(rawGene.indexOf(" ") + 1);
        keyterms.add(new Keyterm(gene));
        previous = gene;
      }
    }
    return keyterms;
  }
}
