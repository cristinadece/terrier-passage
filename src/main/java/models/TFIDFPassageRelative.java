package models;

import org.terrier.matching.models.TF_IDF;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;

public class TFIDFPassageRelative extends TF_IDF{

    /**
     * This overrides the scoring of the WeightingModel (extended by TFIDF).
     *
     * We set passage delimiters:
     *      passage.len.fraction.begin
     *      passage.len.fraction.end
     * e.g. [0,0.25) [0.25,0.5) [0.5,0.75) [0.75, 1]
     *
     * We recalculate tf and doc length according to the limits of the passage
     * and pass it to the TFIDF function
     *
     * @param p the posting list
     * @return the TFIDF score after virtually altering the posting list according to
     * passage delimiters
     */

    @Override
    public double score(Posting p) {

        float fractionBegin = Float.parseFloat(System.getProperty("passage.len.fraction.begin"));
        float fractionEnd = Float.parseFloat(System.getProperty("passage.len.fraction.end"));

//        float fractionBegin = 0.0f;
//        float fractionEnd = 0.25f;
        System.out.println("Frac begin: ".concat(String.valueOf(fractionBegin)));
        System.out.println("Frac end: ".concat(String.valueOf(fractionEnd)));

        int doc_len = p.getDocumentLength();
        int positions[] = ((BlockPosting) p).getPositions();

        int tf = 0;
        int passageBeginIndex = (int) (fractionBegin * doc_len);
        int passageEndIndex = (int) (fractionEnd * doc_len);
        // we increment by 1 to be inclusive of the last word of the doc
        if (passageEndIndex == doc_len) passageEndIndex++;

        for(int i=0; i< positions.length; i++) {
            if ((positions[i] >= passageBeginIndex) && (positions[i] < passageEndIndex)) tf++;
        }

        return super.score(tf, passageEndIndex-passageBeginIndex);
    }


    //TODO: recalculate IDF!
//    private double my_score(double tf) {
//        double Robertson_tf = this.k_1 * tf / (tf + this.k_1 * (1.0D - this.b + this.b * docLength / this.averageDocumentLength));
//        double idf = WeightingModelLibrary.log(this.numberOfDocuments / this.documentFrequency + 1.0D);
//        return this.keyFrequency * Robertson_tf * idf;
//    }


    public static void main(String[] args){
        System.out.println("This is TF_IDF");
    }
}
