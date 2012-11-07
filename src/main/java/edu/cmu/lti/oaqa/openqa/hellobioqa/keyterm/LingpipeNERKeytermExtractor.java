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
public class LingpipeNERKeytermExtractor extends AbstractKeytermExtractor {

  private Chunker chunker;
  
  @Override
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    try {
      chunker = (Chunker) AbstractExternalizable.readObject(new File("src/main/resources/model/ne-en-bio-genetag.hmmchunker"));
    } catch (Exception e) {
      // don't case about different types of Exceptions
      e.printStackTrace();
    }
  }

  @Override
  protected List<Keyterm> getKeyterms(String question) {
    List<Keyterm> keyterms = new ArrayList<Keyterm>();
    Chunking chunking = chunker.chunk(question);
    for (Chunk c : chunking.chunkSet()) {
      String gene = question.substring(c.start(), c.end());
      keyterms.add(new Keyterm(gene));
    }
    return keyterms;
  }
}
