package models;

import org.terrier.matching.models.BM25;
import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

public class BM25P extends WeightingModel{

    /**
     * Splits each document in 4 equal parts
     */

    private static final long serialVersionUID = 1L;
    private double k_1 = 1.2D;
    private double k_3 = 8.0D;
    private double b = 0.75D;
    double w1 = 1, w2 = 1, w3 = 1, w4 = 1;

    public BM25P() {

        k_1 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_1", "1.2"));
        k_3 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_3", "8"));
        b = Double.parseDouble(ApplicationSetup.getProperty("bm25p.b", "0.75"));
        w1 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.w1", "1"));
        w2 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.w2", "1"));
        w3 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.w3", "1"));
        w4 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.w4", "1"));

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
        int q1 = 1 * (docLen / 4);
        int q2 = 2 * (docLen / 4);
        int q3 = 3 * (docLen / 4);
        int q4 = 4 * (docLen / 4); //todo: be careful...this is a division between integers!

        int tf_q1 = 0, tf_q2 = 0, tf_q3 = 0, tf_q4 = 0;

        int positions[] = ((BlockPosting) p).getPositions();

        for(int i=0; i< positions.length; i++) {

            int pos = positions[i];
            if (pos < q1) tf_q1++;
            else if (pos < q2) tf_q2++;
            else if (pos < q3) tf_q3++;
            else tf_q4++;

        }

        double tf = w1 * tf_q1 + w2 * tf_q2 + w3 * tf_q3 + w4 * tf_q4;
        return score(tf, docLen);
    }

}
