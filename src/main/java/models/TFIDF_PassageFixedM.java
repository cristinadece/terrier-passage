package models;

import org.terrier.matching.models.TF_IDF;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.structures.Index;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

import java.io.IOException;

public class TFIDF_PassageFixedM extends TF_IDF{

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
    public double score(Posting p) {

        maxDocLen = Integer.parseInt(ApplicationSetup.getProperty("passage.max.doc.len", "250"));
        int doc_len = p.getDocumentLength();
        int positions[] = ((BlockPosting) p).getPositions();
        int tf = 0;

        for(int i=0; i< positions.length; i++) {
            if ((positions[i] < maxDocLen)) tf++;
        }

        if (doc_len < maxDocLen){ maxDocLen = doc_len;}

        //TODO: these are hardcoded here, since the original attributes from the superclass are private
        double k_1 = 1.2D;
        double b = 0.75D;

        if (_df == null) {

            int df = 0;

            int term_id = this.es.getTermId();
            Index index = Index.createIndex();
            try {
                IterablePosting postings = index.getInvertedIndex().getPostings(index.getLexicon().getLexiconEntry(term_id).getValue());
                while (!postings.endOfPostings()){

                    postings.next();
                    int _positions[] = ((BlockPosting) postings).getPositions();

                    for(int i=0; i< positions.length; i++) {
                        if ((positions[i] < maxDocLen)) {
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

        double Robertson_tf = k_1 * tf / (tf + k_1); //todo: is it ok? boh...
        double idf = WeightingModelLibrary.log(this.numberOfDocuments / _df + 1.0D);
        return this.keyFrequency * Robertson_tf * idf;
    }

    //TODO: recalculate IDF!
    private double my_score(double tf, int[] positions, int maxDocLen) {
        //TODO: recount document frequency for virtual document
        // recount Doc Freq by looking at the positions list in the postings

        // this is with smoothing
        double idf = WeightingModelLibrary.log(this.numberOfDocuments / this.documentFrequency + 1.0D);
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
