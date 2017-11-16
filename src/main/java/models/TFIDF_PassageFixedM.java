package models;

import org.terrier.matching.models.TF_IDF;
import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.Index;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

import java.io.IOException;

public class TFIDF_PassageFixedM extends WeightingModel {

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

    Integer _df = null;


    @Override
    public String getInfo() {
        return "TFIDF Matteo";
    }

    @Override
    public double score(Posting p) {

        maxDocLen = Integer.parseInt(ApplicationSetup.getProperty("passage.max.doc.len", "250"));
        int positions[] = ((BlockPosting) p).getPositions();
        int tf = 0;
        for (int i = 0; i < positions.length; i++) {
            if ((positions[i] < maxDocLen)) tf++;
        }

        return score(tf, 0);
    }

    @Override
    public double score(double tf, double v1) {



        if (_df == null) {

            int df = 0;

            int term_id = this.es.getTermId();
            Index index = Index.createIndex();
            try {
                IterablePosting postings = index.getInvertedIndex().getPostings(index.getLexicon().getLexiconEntry(term_id).getValue());
                while (!postings.endOfPostings()) {

                    postings.next();
                    int currPos[] = ((BlockPosting) postings).getPositions();

                    for (int i = 0; i < currPos.length; i++) {
                        if ((currPos[i] < maxDocLen)) {
                            df++;
                            break;
                        }
                    }
                }
                _df = df;

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        double idf = WeightingModelLibrary.log(this.numberOfDocuments / _df + 1.0D);
        return this.keyFrequency * tf * idf;
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
