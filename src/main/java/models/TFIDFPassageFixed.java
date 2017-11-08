package models;

import org.terrier.matching.models.TF_IDF;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

public class TFIDFPassageFixed extends TF_IDF{

    int maxDocLen = 250;

    /**
     * This overrides the scoring of the WeightingModel (extended by TFIDF).
     *
     * We set passage delimiters:
     *  - maxDocLen - fixed virtual doc leng - regardless of how long the document len is,
     *  we consider maxDocLen or the actual docLen if docLen < maxDocLen
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

        int maxDocLen = Integer.parseInt(ApplicationSetup.getProperty("passage.max.doc.len", "250"));
        int doc_len = p.getDocumentLength();
        int positions[] = ((BlockPosting) p).getPositions();
        int tf = 0;

        for(int i=0; i< positions.length; i++) {
            if ((positions[i] < maxDocLen)) tf++;
        }

        if (doc_len < maxDocLen){ maxDocLen = doc_len;}

        return super.score(tf, maxDocLen);
    }

    //TODO: recalculate IDF!
    private double my_score(double tf) {
        // this is with smoothing
        double idf = WeightingModelLibrary.log(this.numberOfDocuments / this.documentFrequency + 1.0D);
        //TODO: recount document frequency for virtual document
        return tf * idf;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");

        result.append(this.getClass().getName() + " Object with default values as {" + NEW_LINE);
        result.append(" passage.max.doc.len: " + maxDocLen + NEW_LINE);
        result.append("}");

        return result.toString();
    }

    public static void main(String[] args){
        System.out.println("This is TF_IDF Passage with Fixed Doc Len!");
    }
}
