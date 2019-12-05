package models;

import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

import java.lang.Math;

public class BM25RT extends WeightingModel{

    /**
     * Splits each document in p equal parts and multiplies the input weights w with alpha
     *
     * BM25PAlpha is the class that implements BM25P with the following params
     * - k1, k3, b are params from BM25
     * - p is the number of passages
     * - lambda -  decay factor for the ISF function
     * - ISF - type of function: parabola, linear, cosine
     */

    private static final long serialVersionUID = 1L;
    private double k_1 = 1.2D;
    private double k_3 = 8.0D;
    private double b = 0.75D;

    private double lambda = 0.0D;
    private String ISF = "linear";

    /**
     * Initializing the parameters and creating the weight vector of size p.
     */
    public BM25RT() {

        k_1 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_1", "1.2"));
        k_3 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_3", "8"));
        b = Double.parseDouble(ApplicationSetup.getProperty("bm25p.b", "0.75"));

        lambda = Double.parseDouble(ApplicationSetup.getProperty("bm25p.lambda", "0"));
        ISF = ApplicationSetup.getProperty("bm25p.ISF", "linear");

    }

    public String getInfo() {
        return "BM25RT";
    }


    public static double ISFParabola(double lambda, int docLen, double position){
        if (position < lambda*docLen){
            return Math.pow((position/(lambda*docLen) -1), 2);
        }
        else{
            return 0.0d;
        }
    }


    public static double ISFLinear(double lambda, int docLen, double position){
        if (position < lambda*docLen){
            return 1-(position/(lambda*docLen));
        }
        else{
            return 0.0d;
        }
    }

    public static double ISFCosine(double lambda, int docLen, double position){
        if (position < lambda*docLen){
            double rad = Math.toRadians((Math.PI*position)/(2*lambda*docLen));
            return Math.cos(rad);
        }
        else{
            return 0.0d;
        }
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
        double tf_RT = 0.0;
        int docLen = posting.getDocumentLength();

        if (docLen > 0) {
            int positions[] = ((BlockPosting) posting).getPositions();
            for (int i = 0; i < positions.length; i++) {
                double pos = (double) positions[i];
                tf++;

                if (ISF.equals("linear")){
                    tf_RT = 1 + ISFLinear(lambda, docLen, pos);
                }
                else if (ISF.equals("cosine")){
                    tf_RT = 1 + ISFLinear(lambda, docLen, pos);
                }
                else if (ISF.equals("parabola")){
                    tf_RT = 1 + ISFLinear(lambda, docLen, pos);
                }
            }
        }

        double k_1_factor = tf_RT/tf;

        return score(tf_RT, docLen);
    }

    @Override
    public String toString() {
        return "BM25RT";
    }


//    public static void main(String args[])
//    {
//
//        System.out.println(ISFParabola(0.2, 100, 10));
//        System.out.println(ISFLinear(0.2, 100, 10));
//        System.out.println(ISFCosine(0.2, 100, 10));
//
//    }
}
