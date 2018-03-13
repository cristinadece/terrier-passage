package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import org.terrier.applications.batchquerying.TRECQuerying;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.models.WeightingModelLibrary;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.IndexOnDisk;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.MetaIndex;
import org.terrier.structures.PostingIndex;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.TerrierTimer;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;



public class HighIDFMatching {

    public static void main(String[] args) throws IOException {
        IndexOnDisk index = Index.createIndex();
        MetaIndex metaIndex = index.getMetaIndex();
        PostingIndex<?> invertedIndex = index.getInvertedIndex();
        int numDocs = index.getCollectionStatistics().getNumberOfDocuments();
        TObjectIntHashMap<String> docno2docid = new TObjectIntHashMap<String>();

        TerrierTimer tt = new TerrierTimer("Loading the metaindex in main memory for reverse lookup", numDocs);
        tt.start();
        for (int docid = 0; docid < numDocs; docid++) {

            docno2docid.put(metaIndex.getItem("docno", docid), docid);
            tt.increment();
        }
        tt.finished();


        PrintWriter pw = new PrintWriter(new FileWriter("HighIDFTermsWithPositions.txt"));
        Iterator<Map.Entry<String, LexiconEntry>> iterator = index.getLexicon().iterator();
        while (iterator.hasNext()) {

            Map.Entry<String, LexiconEntry> entry = iterator.next();
            String term = entry.getKey();
            LexiconEntry lexiconEntry = entry.getValue();
            double df = lexiconEntry.getDocumentFrequency();
            double idf = WeightingModelLibrary.log(numDocs / lexiconEntry.getDocumentFrequency() + 1.0D);

            if ((df>=50) && (idf >= 14.0)){
                IterablePosting postings = invertedIndex.getPostings(lexiconEntry);
                postings.next(); //initialisation (because posting list starts from -1)
                int[] termPositions = ((BlockPosting) postings).getPositions();

                while (!postings.endOfPostings()){
                    int docid = postings.getId();
                    termPositions = ((BlockPosting) postings).getPositions();
                    printPositions(term, docid, Arrays.toString(termPositions), pw);
                    postings.next();
                }
            }
        }
        pw.close();
    }

    private static void printPositions(String term, int docid, String termPosting, PrintWriter pw) throws IOException {
        pw.printf("%s\t%d\t%s\n",
                term,
                docid,
                termPosting);
    }

}
