package models;

import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

import java.util.Arrays;


public class DLMPassage extends WeightingModel {

    private static final long serialVersionUID = 1L;
    private int alpha = 10;
    private int p = 10;
    String w = "[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]";
    double[] weights = new double[p]; // this will change to a different size in constructor


    public DLMPassage() {
        super();
        c = 2500;

        alpha = Integer.parseInt(ApplicationSetup.getProperty("bm25p.alpha", "10"));
        p = Integer.parseInt(ApplicationSetup.getProperty("bm25p.p", "10"));
        w = ApplicationSetup.getProperty("bm25p.w", "[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]");

        weights = new double[p];
        System.out.println("Number of passages: " + String.valueOf(p));
        String[] split_w = w.replace("[","").replace("]", "").split(",");
        weights = Arrays.stream(split_w)
                .mapToDouble(Double::parseDouble)
                .toArray();
    }


    public double score(double tf, double docLength) {
        return WeightingModelLibrary.log(1 + (tf/(c * (super.termFrequency / numberOfTokens))) ) + WeightingModelLibrary.log(c/(docLength+c));
    }


    @Override
    public double score(Posting posting) {

        double tf = 0.0;
        int docLen = posting.getDocumentLength();

        int[] tf_passage = new int[p]; //initialized to 0

        if (docLen > 0) {
            int positions[] = ((BlockPosting) posting).getPositions();
            for (int i = 0; i < positions.length; i++) {
                double pos = (double) positions[i];
                tf_passage[(int) Math.floor(pos * p / docLen)]++;
            }

            for (int i = 0; i < p; i++) {
                tf += alpha * weights[i] * tf_passage[i];
            }
        }

        return score(tf, docLen);
    }


    public String getInfo() {
        return "DLMPassage";
    }



}
