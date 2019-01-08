package models;

import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

import java.util.Arrays;

public class BM25PExtended extends WeightingModel{

    /**
     * Splits each document in p equal parts and multiplies the input weights w with alpha
     *
     * BM25PAlpha is the class that implements BM25P with the following params
     * - k1, k3, b are params from BM25
     * - p is the number of passages
     * - alpha is the multiplication factor for the weights distribution
     * - w is the list of weights (read as string)
     */

    private static final long serialVersionUID = 1L;
    private double k_1 = 1.2D;
    private double k_3 = 8.0D;
    private double b = 0.75D;
    private int alpha = 1;
    private int p = 100;
    private int percentage = 100;
    private int offset = 30;
    double[] weights = new double[p]; // this will change to a different size in constructor


    /**
     * Initializing the parameters and creating the weight vector of size p.
     */
    public BM25PExtended() {

        k_1 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_1", "1.2"));
        k_3 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_3", "8"));
        b = Double.parseDouble(ApplicationSetup.getProperty("bm25p.b", "0.75"));
        alpha = Integer.parseInt(ApplicationSetup.getProperty("bm25p.alpha", "1"));
        p = Integer.parseInt(ApplicationSetup.getProperty("bm25p.p", "100"));
        percentage = Integer.parseInt(ApplicationSetup.getProperty("bm25p.percent", "100"));
        offset = Integer.parseInt(ApplicationSetup.getProperty("bm25p.offset", "30"));


        weights = new double[p];
        System.out.println("Number of passages: " + String.valueOf(p));
        Arrays.fill(weights, 0.0); // init the array of weights to 0.0 for each weight
        // we start adding 1 up to a certain percentage
        if (percentage <= weights.length) {
            for (int i = 0; i < percentage; i++) {
                //Case A - 1 .. 100
//                weights[i] = 1.0; // Incremental from 1..100

                //Case B - 100 .. 1
//                weights[weights.length-1-i] = 1.0; // Decremental from 100..1

                //Case C - 20,21 ... 18, 19 - we start from offset
                if (offset + i < weights.length){
                    weights[offset + i] = 1.0;
                }
                else {
                    weights[offset + i - percentage] = 1.0;
                }
            }
        }
        else{
            System.out.println("Percentage is bigger than the weights vector! Error!");
        }
    }

    public String getInfo() {
        return "BM25PExtended";
    }

    public double score(double tf, double docLength) {
        //this is plain bm25 copied from TERRIER, 'cos no code reutilisation was possible
        double K = this.k_1 * (1.0D - this.b + this.b * docLength / this.averageDocumentLength);

        return WeightingModelLibrary.log((this.numberOfDocuments - this.documentFrequency + 0.5D) /
                (this.documentFrequency + 0.5D)) * ((this.k_1 + 1.0D) * tf / (K + tf)) *
                ((this.k_3 + 1.0D) * this.keyFrequency / (this.k_3 + this.keyFrequency));
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

    @Override
    public String toString() {
        return "BM25PExtended";
    }
}
