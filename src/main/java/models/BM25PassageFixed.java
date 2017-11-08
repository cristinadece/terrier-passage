package models;

import org.terrier.matching.models.BM25;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;


public class BM25PassageFixed extends BM25 {

    int maxDocLen = 250;

    /**
     * This overrides the scoring of the WeightingModel (extended by BM25).
     *
     * We set passage delimiters:
     *  - maxDocLen - fixed virtual doc leng - regardless of how long the document len is,
     *  we consider maxDocLen or the actual docLen if docLen < maxDocLen
     *
     * We recalculate tf and doc length according to the limits of the passage
     * and pass it to the BM25 function
     *
     * @param p the posting list
     * @return the BM25 score after virtually altering the posting list according to
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
        System.out.println("This is BM25 Passage with Fixed Doc Len!");
    }

}
