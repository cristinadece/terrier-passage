package models;

import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.aftereffect.AfterEffect;
import org.terrier.matching.models.basicmodel.BasicModel;
import org.terrier.matching.models.normalisation.Normalisation;

import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

import java.util.Arrays;

public class DFRPassage extends WeightingModel {
    /**
     *
     * https://jar-download.com/artifacts/org.terrier/terrier-core/5.0/source-code/org/terrier/matching/models/DFRWeightingModel.java
     * http://terrier.org/docs/v4.1/javadoc/org/terrier/matching/models/WeightingModel.html
     * http://terrier.org/docs/v4.1/javadoc/org/terrier/matching/models/DFRWeightingModel.html#score(double,%20double)
     * http://terrier.org/docs/v4.1/dfr_description.html
     *
     * Splits each document in p equal parts and multiplies the input weights w with alpha
     *
     * - p is the number of passages
     * - alpha is the multiplication factor for the weights distribution
     * - w is the list of weights (read as string)
     */

    protected BasicModel basicModel;
    /** The applied model for after effect (aka. first normalisation). */
    protected AfterEffect afterEffect;
    /** The applied frequency normalisation method. */
    protected Normalisation normalisation;
    /** The prefix of the package of the frequency normalisation methods. */
    protected final String NORMALISATION_PREFIX = "org.terrier.matching.models.normalisation.Normalisation";
    /** The prefix of the package of the basic models for randomness. */
    protected final String BASICMODEL_PREFIX = "org.terrier.matching.models.basicmodel.";
    /** The prefix of the package of the first normalisation methods by after effect. */
    protected final String AFTEREFFECT_PREFIX = "org.terrier.matching.models.aftereffect.";
    /** The parameter of the frequency normalisation component. */
    protected double parameter;
    /** A boolean that indicates if the frequency normalisation is enabled. */
    protected boolean ENABLE_NORMALISATION;
    /** A boolean that indicates if the first normalisation by after effect
     * is enabled. */
    protected boolean ENABLE_AFTEREFFECT;


    private static final long serialVersionUID = 1L;
    private int alpha = 10;
    private int p = 10;
    String w = "[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]";
    double[] weights = new double[p]; // this will change to a different size in constructor


    /**
     * Initializing the parameters and creating the weight vector of size p.
     */
    public DFRPassage() {

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


    @Override
    public String getInfo() {
        String modelName = this.basicModel.getInfo();
        if (ENABLE_AFTEREFFECT)
            modelName += this.afterEffect.getInfo();
        if (this.ENABLE_NORMALISATION)
            modelName += this.normalisation.getInfo();
        return modelName;
    }


    /**
     * Compute a weight for a term in a document.
     * @param tf The term frequency in the document
     * @param docLength the document's length
     * @return the score assigned to a document with the given
     *         tf and docLength, and other preset parameters
     */
    public double score(double tf, double docLength) {
        double tfn = tf;
        // if the frequency normalisation is enabled, do the normalisation.
        if (this.ENABLE_NORMALISATION)
            tfn = normalisation.normalise(tf, docLength, termFrequency);
        double gain = 1;
        // if the first normalisation by after effect is enabled, compute the gain.
        if (this.ENABLE_AFTEREFFECT)
            gain = afterEffect.gain(tfn, documentFrequency, termFrequency);
        // produce the final score.
        return  gain *
                basicModel.score(tfn,
                        documentFrequency,
                        termFrequency,
                        keyFrequency,
                        docLength);
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
        return "DFRPassage";
    }


}
