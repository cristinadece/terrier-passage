package models;

import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

public class BM25Pexp extends WeightingModel{
    /**
     * The class changes
     */

    private static final long serialVersionUID = 1L;
    private double k_1 = 1.2D;
    private double k_3 = 8.0D;
    private double b = 0.75D;
    public double w1 = 1, w2 = 1, w3 = 1, w4 = 1, w5 = 1, w6 =1;
    String w = "111111";

    public BM25Pexp() {

        k_1 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_1", "1.2"));
        k_3 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_3", "8"));
        b = Double.parseDouble(ApplicationSetup.getProperty("bm25p.b", "0.75"));
        w = ApplicationSetup.getProperty("bm25p.w", "111111");
        w1 = Double.parseDouble(w.substring(0,1));
        w2 = Double.parseDouble(w.substring(1,2));
        w3 = Double.parseDouble(w.substring(2,3));
        w4 = Double.parseDouble(w.substring(3,4));
        w5 = Double.parseDouble(w.substring(4,5));
        w6 = Double.parseDouble(w.substring(5,6));

    }

    public String getInfo() {
        return "BM25P";
    }

    public double score(double tf, double docLength) {
        //this is plain bm25 copied from TERRIER, 'cos no code reutilisation was possible
        double K = this.k_1 * (1.0D - this.b + this.b * docLength / this.averageDocumentLength);
        return WeightingModelLibrary.log((this.numberOfDocuments - this.documentFrequency + 0.5D)
                / (this.documentFrequency + 0.5D)) * ((this.k_1 + 1.0D) * tf / (K + tf)) *
                ((this.k_3 + 1.0D) * this.keyFrequency / (this.k_3 + this.keyFrequency));
    }

    @Override
    public double score(Posting p) {

        int docLen = p.getDocumentLength();
        int q1 = 100;
        int q2 = 200;
        int q3 = 400;
        int q4 = 600;
        int q5 = 1000;

        int tf_q1 = 0, tf_q2 = 0, tf_q3 = 0, tf_q4 = 0, tf_q5 = 0, tf_q6 =0;

        int positions[] = ((BlockPosting) p).getPositions();

        for(int i=0; i< positions.length; i++) {

            int pos = positions[i];
            if (pos < q1) tf_q1++;
            else if (pos < q2) tf_q2++;
            else if (pos < q3) tf_q3++;
            else if (pos < q4) tf_q4++;
            else if (pos < q5) tf_q5++;
            else tf_q6++;

        }

        double tf = w1 * tf_q1 + w2 * tf_q2 + w3 * tf_q3 + w4 * tf_q4 + w5 *tf_q5 + w6 * tf_q6;
        return score(tf, docLen);
    }

	@Override
	public String toString() {
		
		return "BM25PExp";
	}

}
