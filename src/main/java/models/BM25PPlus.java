package models;

import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

public class BM25PPlus extends WeightingModel{

    /**
     * Applies a decreasing function over the weight of each passage (passages have
     * fixe length)
     */

    private static final long serialVersionUID = 1L;
    private double k_1 = 1.2D;
    private double k_3 = 8.0D;
    private double b = 0.75D;
    double w1 = 1, w2 = 1, w3 = 1, w4 = 1;
    private int passLen = 250;

    public BM25PPlus() {

        k_1 = Double.parseDouble(ApplicationSetup.getProperty("bm25pplus.k_1", "1.2"));
        k_3 = Double.parseDouble(ApplicationSetup.getProperty("bm25pplus.k_3", "8"));
        b = Double.parseDouble(ApplicationSetup.getProperty("bm25pplus.b", "0.75"));
        passLen = Integer.parseInt(ApplicationSetup.getProperty("bm25pplus.passlen", "250"));
    }

    public String getInfo() {
        return "BM25P";
    }

    public double score(double tf, double docLength) {
        //this is plain bm25 copied from TERRIER, 'cos no code reutilisation was possible
        double K = this.k_1 * (1.0D - this.b + this.b * docLength / this.averageDocumentLength);
        return WeightingModelLibrary.log((this.numberOfDocuments - this.documentFrequency + 0.5D) / (this.documentFrequency + 0.5D)) * ((this.k_1 + 1.0D) * tf / (K + tf)) * ((this.k_3 + 1.0D) * this.keyFrequency / (this.k_3 + this.keyFrequency));
    }

    @Override
    public double score(Posting p) {

        int docLen = p.getDocumentLength();

        int positions[] = ((BlockPosting) p).getPositions();

        double tf = 0;

        for(int i=0; i< positions.length; i++) {

            int pos = positions[i];
            int passage = (pos / passLen) + 1;
            tf += 1.0 + (1.0 / passage);
        }

        return score(tf, docLen);
    }

}
