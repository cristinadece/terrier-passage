package matching;


import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.QueryResultSet;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.WeightingModel;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.*;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.ApplicationSetup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class RankedOrTFPrint
{			
	static final Logger LOGGER = LoggerFactory.getLogger(RankedOrTFPrint.class);
	static boolean IGNORE_LOW_IDF_TERMS = Boolean.parseBoolean(ApplicationSetup.getProperty("ignore.low.idf.terms","true"));
	
	static class Tuple implements EntryStatistics
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		IterablePosting posting = null;
		int doc_freq_in_coll = 0; // need this for scoring
		int term_freq_in_coll = 0; // need this for scoring
		
		Tuple(IterablePosting posting, int doc_freq_in_coll, int term_freq_in_coll)
		{
			this.posting = posting;
			this.doc_freq_in_coll = doc_freq_in_coll;
			this.term_freq_in_coll = term_freq_in_coll;
		}
		
		@Override
		public String toString()
		{
			return posting.toString() + ", [" + doc_freq_in_coll + "," + term_freq_in_coll + "]"; 
		}

		@Override
		public int getFrequency() {
			
			return term_freq_in_coll;
		}

		@Override
		public int getDocumentFrequency() {
			
			return doc_freq_in_coll;
		}

		@Override
		public int getTermId() {
			
			return -1;
		}

		@Override
		public void add(EntryStatistics e) {

			throw new UnsupportedOperationException();
		}

		@Override
		public void subtract(EntryStatistics e) {
			throw new UnsupportedOperationException();			
		}

		@Override
		public EntryStatistics getWritableEntryStatistics() {
			throw new UnsupportedOperationException();
		}
	}

	private Index mIndex = null;
	private DocumentIndex mDocIndex = null;
	private MetaIndex mMetaIndex = null;
	private WeightingModel mWeightingModel = null;
			
	public String getInfo() 
	{
		return "Ranked Or processing (should be another implementation of DAAT Full)";
	}

	public void close() throws IOException 
	{
	}


	public void setup(final Index index, final WeightingModel weightingModel) 
	{
		checkNotNull(index);
		checkNotNull(weightingModel);
		
		this.mIndex = index;
		this.mDocIndex = index.getDocumentIndex();
		this.mMetaIndex = index.getMetaIndex();
		this.mWeightingModel = weightingModel;
	}


	@SuppressWarnings({ })
	public ResultSet match(final SearchRequest searchRequest, BufferedWriter bw) throws IOException
	{
		checkNotNull(searchRequest);

		int TOP_K = Integer.parseInt(ApplicationSetup.getProperty("matching.trecresults.length", "1000"));
		
		//ObjectList<Pair<IterablePosting, Integer>> enums = Matching.Simple.LookAndSort(mIndex, searchRequest); 
		ObjectList<Tuple> enums = LookAndSort(mIndex, searchRequest);
		if (enums.size() == 0) {
			return new QueryResultSet(0);
		}

		String qid = searchRequest.getQueryID();
		int[] documentFrequencies = new int[enums.size()];
		for (int i = 0; i < enums.size(); i++) documentFrequencies[i] = enums.get(i).doc_freq_in_coll;
		double avgDocLen = mIndex.getCollectionStatistics().getAverageDocumentLength();
		int N = mIndex.getCollectionStatistics().getNumberOfDocuments();
		int q1 = 100;
		int q2 = 200;
		int q3 = 400;
		int q4 = 600;
		int q5 = 1000;

		TopQueue heap = new TopQueue(TOP_K, 0.0f);
		
		Set<Tuple> toRemove = new HashSet<>();
		int currentDocid = InitializeAndSelectMinimumDocId(enums);		
        while (enums.size() > 0) {
        	toRemove.clear();
        	//System.err.println(currentDocid);
        	Result result = new Result(currentDocid);
        	int nextDocid = Integer.MAX_VALUE;
        	List<int[]> positions = new ArrayList<>();
        	for (int i = 0; i < enums.size(); i++) {
        		IterablePosting p = enums.get(i).posting;
        		if (p.getId() == currentDocid) {
					positions.add(((BlockPosting) p).getPositions());
        			result.updateScore((float) mWeightingModel.score(p.getFrequency(), p.getDocumentLength()));
        			if (p.endOfPostings()) {
        				toRemove.add(enums.get(i));
        			} else {        				
        				p.next();
        			}
        		}
        		if (p.getId() < nextDocid) 
        			nextDocid = p.getId();
        	}

			// TODO: write tfs to file
			int docid = currentDocid;
        	String docno = mMetaIndex.getItem("docno", docid);
			int doclen = mDocIndex.getDocumentLength(docid);

			int tf_q1 = 0, tf_q2 = 0, tf_q3 = 0, tf_q4 = 0, tf_q5 = 0, tf_q6 =0;

			for (int[] positionArray : positions) {

				for (int pos : positionArray) {

					if (pos < q1) tf_q1++;
					else if (pos < q2) tf_q2++;
					else if (pos < q3) tf_q3++;
					else if (pos < q4) tf_q4++;
					else if (pos < q5) tf_q5++;
					else tf_q6++;

				}

			}

			//qid, N, dfs, avgDL, docid, docno, doclen, rel, positions
			String line = String.format("%s\t%d\t%s\t%.5f\t%d\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n",
					qid,
					N,
					Arrays.toString(documentFrequencies),
					avgDocLen,
					docid,
					docno,
					doclen,
					tf_q1, tf_q2, tf_q3, tf_q4, tf_q5, tf_q6);
			bw.write(line);


        	currentDocid = nextDocid;
        	for (Tuple t : toRemove) enums.remove(t);
        }


        if (heap.isEmpty()) {

        	return new QueryResultSet(0);

        } else {
        	
        	int[] docids = new int[heap.size()];
        	double[] scores = new double[heap.size()];
        	short[] occurrences = new short[heap.size()]; //watch out, this is 1 filled!, it's a dummy variable

        	int idx = 0;
        	while (!heap.isEmpty()) {

        		Result res = heap.getTop().dequeue();
        		docids[idx] = res.getDocId();
        		scores[idx] = res.getScore();
        		occurrences[idx] = 1;
        		idx++;
        	}

        	return new QueryResultSet(docids, scores, occurrences);
        }
	}
	
	final static ObjectList<Tuple> LookAndSort(final Index mIndex, final SearchRequest searchRequest) throws IOException
	{
		int numDocs = mIndex.getCollectionStatistics().getNumberOfDocuments();

		ObjectList<Tuple> enums = new ObjectArrayList<Tuple>();
		
		// We look in the index and filter out common terms
        MatchingQueryTerms mqt = new MatchingQueryTerms(searchRequest.getOriginalQuery());
        searchRequest.getQuery().obtainQueryTerms(mqt);
        String[] terms = mqt.getTerms();		
		for (String term: terms) {
			LexiconEntry t = mIndex.getLexicon().getLexiconEntry(term);
			if (t == null) {
				LOGGER.warn("Term not found in index: " + term);
			} else if (IGNORE_LOW_IDF_TERMS && t.getFrequency() > numDocs) {
				LOGGER.warn("Term " + term + " has low idf - ignored from scoring.");
			} else {
				IterablePosting ip = mIndex.getInvertedIndex().getPostings(t);
				ip.next();
				enums.add(new Tuple(ip, t.getDocumentFrequency(), t.getFrequency()));
			}
		}
		
		// We sort the query terms from shortest to longest.
		Collections.sort(enums, new Comparator<Tuple>() {
			@Override
			public int compare(Tuple o1, Tuple o2) {
		        return Integer.compare(o1.doc_freq_in_coll, o2.doc_freq_in_coll);
		    }
		});
		
		return enums;
	}

	final static int InitializeAndSelectMinimumDocId(final ObjectList<Tuple> enums) 
	{
		int docid = Integer.MAX_VALUE;
		for (int i = 0; i < enums.size(); i++) {
			int _id = enums.get(i).posting.getId();
			if (_id < docid)
				docid = _id;
		}
		return docid;
	}

}
