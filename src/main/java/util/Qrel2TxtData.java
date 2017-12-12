package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.terrier.applications.batchquerying.TRECQuerying;
import org.terrier.matching.MatchingQueryTerms;
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

class RelevanceAssessment implements Comparable<RelevanceAssessment> {

    public int docid;
    public int relevance;

    public RelevanceAssessment(int docid, int relevance) {

        this.docid = docid;
        this.relevance = relevance;

    }

    @Override
    public int compareTo(RelevanceAssessment o) {
        return Integer.compare(docid, o.docid);
    }
}

public class Qrel2TxtData extends TRECQuerying {

    public Qrel2TxtData() {

        super();

    }

    @SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {

        //SETUP

        Qrel2TxtData fb = new Qrel2TxtData();

        IndexOnDisk index = Index.createIndex();

        MetaIndex metaIndex = index.getMetaIndex();

        int numDocs = index.getCollectionStatistics().getNumberOfDocuments();

        TObjectIntHashMap<String> docno2docid = new TObjectIntHashMap<String>();

        TerrierTimer tt = new TerrierTimer("Loading the metaindex in main memory for reverse lookup", numDocs);
        tt.start();
        for (int docid = 0; docid < numDocs; docid++) {

            docno2docid.put(metaIndex.getItem("docno", docid), docid);
            tt.increment();
        }
        tt.finished();


        TIntObjectHashMap<String[]> qid2terms = new TIntObjectHashMap<>();
        while (fb.querySource.hasNext()) {


            String query = fb.querySource.next();
            String qidString = fb.querySource.getQueryId();
            int qid = Integer.parseInt(qidString);

            SearchRequest searchRequest = fb.queryingManager.newSearchRequest(
                    qidString, query);

            MatchingQueryTerms mqt = new MatchingQueryTerms(qidString);
            searchRequest.getQuery().obtainQueryTerms(mqt);
            String[] terms = mqt.getTerms();


            qid2terms.put(qid, terms);
        }


        String qrelName = args[0];
        BufferedReader br = new BufferedReader(new FileReader(qrelName));
        String line;
        TIntObjectHashMap<List<RelevanceAssessment>> qid2qrel = new TIntObjectHashMap<>();
        while ((line = br.readLine()) != null) {

            String[] fields = line.split(" ");
            int qid = Integer.parseInt(fields[0]);
            String docno = fields[2];
            if (!docno2docid.contains(docno)) continue;
            int docid = docno2docid.get(docno);
            int relevance = Integer.parseInt(fields[3]);


            List<RelevanceAssessment> assessments;
            if (qid2qrel.containsKey(qid)) {
                assessments = qid2qrel.get(qid);
            } else {
                assessments = new ArrayList<>();
                qid2qrel.put(qid, assessments);
            }
            assessments.add(new RelevanceAssessment(docid, relevance));
        }
        br.close();
        
        for (Object ra : qid2qrel.getValues())
            Collections.sort((List<RelevanceAssessment>) ra);

        //THE REAL THING
        printPositions(index, metaIndex, docno2docid, qid2terms, qid2qrel);
    }

    private static void printPositions(IndexOnDisk index, MetaIndex metaIndex, TObjectIntHashMap<String> docno2docid, TIntObjectHashMap<String[]> qid2terms, TIntObjectHashMap<List<RelevanceAssessment>> qid2qrel) throws IOException {

        Lexicon<String> lexicon = index.getLexicon();
        PostingIndex<?> invertedIndex = index.getInvertedIndex();
        DocumentIndex documentIndex = index.getDocumentIndex();
        int N = index.getCollectionStatistics().getNumberOfDocuments();
        double avgDocLen = index.getCollectionStatistics().getAverageDocumentLength();

        PrintWriter pw = new PrintWriter(new FileWriter("output.txt"));

        for (int qid : qid2terms.keys()) {

            String[] terms = qid2terms.get(qid);
            List<RelevanceAssessment> assessments = qid2qrel.get(qid);
            if (assessments == null) continue;
            List<IterablePosting> postingLists = new ArrayList<>();
            List<Integer> documentFrequencyList = new ArrayList<>();

            for (String t : terms) {

                LexiconEntry lexiconEntry = lexicon.getLexiconEntry(t);
                if (lexiconEntry != null) {

                    IterablePosting postings = invertedIndex.getPostings(lexiconEntry);
                    postings.next(); //initialisation (because posting list starts from -1)
                    postingLists.add(postings);
                    documentFrequencyList.add(lexiconEntry.getDocumentFrequency());
                }

            }
            
            int[] documentFrequencies = new int[documentFrequencyList.size()];
            for (int i = 0; i < documentFrequencies.length; i++) 
            	documentFrequencies[i] = documentFrequencyList.get(i);

            for (RelevanceAssessment ra : assessments) {

                int docid = ra.docid;
                int doclen = documentIndex.getDocumentLength(docid);
                int[][] qtermPositions = new int[postingLists.size()][0];

                for (int idx = 0; idx < postingLists.size(); idx++) {

                    IterablePosting ip = postingLists.get(idx);
                    if (docid > ip.getId()) ip.next(docid); //go to next docid greater equal than docid
                    if (docid == ip.getId()) qtermPositions[idx] = ((BlockPosting) ip).getPositions(); //if you are on docid, then do your thing
                }

                //qid, N, dfs, avgDL, docid, docno, doclen, rel, positions
                pw.printf("%d\t%d\t%s\t%d\t%s\t%d\t%.5f\t%d\t%s\n", 
                		qid,
                		N,
                		Arrays.toString(documentFrequencies),
                		docid, 
                		metaIndex.getItem("docno", docid), 
                		doclen,
                		avgDocLen,
                		ra.relevance, 
                		Arrays.deepToString(qtermPositions));
            }

        }
        
	pw.close();
    }
}
