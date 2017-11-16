package models;

import org.terrier.matching.models.TF_IDF;
import org.terrier.structures.Index;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;

import java.io.IOException;

public class TFIDF_PassageFixedM extends TF_IDF {

    int maxDocLen = 250;


    public TFIDF_PassageFixedM() {

        maxDocLen = Integer.parseInt(ApplicationSetup.getProperty("passage.max.doc.len", "250"));
    }

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

        int positions[] = ((BlockPosting) p).getPositions();
        int tf = 0;
        for (int i = 0; i < positions.length; i++) {
            if ((positions[i] < maxDocLen)) tf++;
        }

        return super.score(tf, Math.min(p.getDocumentLength(), maxDocLen));
    }

    public void prepare() {

        super.prepare();
        this.averageDocumentLength = maxDocLen;
        double df = this.documentFrequency;
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
            this.documentFrequency = df;

        } catch (IOException e) {
            e.printStackTrace();
        }

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
