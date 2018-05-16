package models;

import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

public class BM25P10 extends WeightingModel{

    /**
     * Splits each document in 10 equal parts
     */

    private static final long serialVersionUID = 1L;
    private double k_1 = 1.2D;
    private double k_3 = 8.0D;
    private double b = 0.75D;
    private int num_passage = 10;
    String w = "[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]";
    double[] weights = new double[num_passage];

    public BM25P10() {

        k_1 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_1", "1.2"));
        k_3 = Double.parseDouble(ApplicationSetup.getProperty("bm25p.k_3", "8"));
        b = Double.parseDouble(ApplicationSetup.getProperty("bm25p.b", "0.75"));
        w = ApplicationSetup.getProperty("bm25p.w", "[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]");
        String[] split_w = w.replace("[","").replace("]", "").split(",");
        weights = Arrays.stream(split_w)
                .mapToDouble(Double::parseDouble)
                .toArray();
    }

    public String getInfo() {
        return "BM25P";
    }

    public double score(double tf, double docLength) {
        //this is plain bm25 copied from TERRIER, 'cos no code reutilisation was possible
        double K = this.k_1 * (1.0D - this.b + this.b * docLength / this.averageDocumentLength);
        System.out.print((String.valueOf(tf / (K + tf))).concat("\t"));
        return WeightingModelLibrary.log((this.numberOfDocuments - this.documentFrequency + 0.5D) /
                (this.documentFrequency + 0.5D)) * ((this.k_1 + 1.0D) * tf / (K + tf)) *
                ((this.k_3 + 1.0D) * this.keyFrequency / (this.k_3 + this.keyFrequency));
    }

    @Override
    public double score(Posting p) {

        double tf = 0.0;
        int docLen = p.getDocumentLength();

        System.out.print(String.valueOf(p.getId()).concat("\t"));
//        System.out.println("Doc len: ".concat(String.valueOf(docLen)));

        if (docLen/num_passage == 0){

            int[] tf_passage = new int[docLen]; //doclen is less than 10

            int positions[] = ((BlockPosting) p).getPositions();
            for (int i = 0; i < positions.length; i++) {
                float pos = positions[i];
                tf_passage[Math.round(pos * (num_passage-1) / docLen)]++;
            }

            for (int i = 0; i < docLen; i++) {
                tf += weights[i] * tf_passage[i];
            }

//            System.out.println("Positions: ".concat(Arrays.toString(positions)));
//            System.out.println("TF array: ".concat(Arrays.toString(tf_passage)));


        }
        else {

            int[] tf_passage = new int[num_passage]; //initialized to 0

            int positions[] = ((BlockPosting) p).getPositions();
            for (int i = 0; i < positions.length; i++) {
                float pos = positions[i];
                tf_passage[Math.round(pos * (num_passage-1) / docLen)]++;
            }

            for (int i = 0; i < num_passage; i++) {
                tf += weights[i] * tf_passage[i];
            }

//            System.out.println("Positions: ".concat(Arrays.toString(positions)));
//            System.out.println("TF array: ".concat(Arrays.toString(tf_passage)));s
        }

        NumberFormat formatter = new DecimalFormat("#0.00");
        System.out.print(formatter.format(tf).concat("\t"));
        double score1 = score(tf, docLen);
        System.out.println(String.valueOf(score1));
        return score1;
    }

    @Override
    public String toString() {
        return "BM25P10";
    }
}
