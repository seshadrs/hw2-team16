package edu.cmu.lti.oaqa.openqa.hellobioqa.keyterm;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.descriptor.ConfigurationParameter;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ConfidenceChunker;
import com.aliasi.util.AbstractExternalizable;

import edu.cmu.lti.oaqa.cse.basephase.keyterm.AbstractKeytermExtractor;
import edu.cmu.lti.oaqa.framework.data.Keyterm;

public class LingPipeKeytermExtractor extends AbstractKeytermExtractor {
  static int MAX_N_BEST_CHUNKS = 7;
  static double THRESHOLD = 0.6; // confidence threshold

  ConfidenceChunker chunker;
  File modelFile;
  
  @Override
  public void initialize(UimaContext c)
      throws ResourceInitializationException {
    super.initialize(c);
    String inputPath = (String) c.getConfigParameterValue("modelfile");
    // Output file should be specified in the descriptor
    if (inputPath == null) {
      throw new ResourceInitializationException(
          ResourceInitializationException.CONFIG_SETTING_ABSENT,
          new Object[] { "modelfile" });
    }
    // If specified output directory does not exist, try to create it
    modelFile = new File(inputPath.trim());
    if (modelFile.exists() == false) {
      throw new ResourceInitializationException(
          ResourceInitializationException.RESOURCE_DATA_NOT_VALID,
          new Object[] { inputPath, "modelFile" });
    }
    System.out.println("Reading chunker from file=" + modelFile);
    try {
      chunker = (ConfidenceChunker) AbstractExternalizable
          .readObject(modelFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected List<Keyterm> getKeyterms(String question) {
    List<Keyterm> keytermStrings = new ArrayList<Keyterm>();
    char[] cs = question.toCharArray();
    Iterator<Chunk> it = chunker.nBestChunks(cs, 0, cs.length,
        MAX_N_BEST_CHUNKS);
    for (int n = 0; it.hasNext(); ++n) {
      Chunk chunk = it.next();
      double conf = Math.pow(2.0, chunk.score());
      int start = chunk.start();
      int end = chunk.end();
      String phrase = question.substring(start, end);
      if (conf > THRESHOLD) {
        Keyterm kt = new Keyterm(phrase);
        keytermStrings.add(kt);
      }
    }
    return keytermStrings;
  }

}
