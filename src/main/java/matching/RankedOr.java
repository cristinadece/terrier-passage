package matching;


import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.QueryResultSet;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.WeightingModel;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.EntryStatistics;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.ApplicationSetup;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class RankedOr
{			
	static final Logger LOGGER = LoggerFactory.getLogger(RankedOr.class);
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
		this.mWeightingModel = weightingModel;
	}

	@SuppressWarnings({ })
	public ResultSet match(final SearchRequest searchRequest) throws IOException 
	{
		checkNotNull(searchRequest);
		
		int TOP_K = Integer.parseInt(ApplicationSetup.getProperty("matching.trecresults.length", "1000"));
		
		//ObjectList<Pair<IterablePosting, Integer>> enums = Matching.Simple.LookAndSort(mIndex, searchRequest); 
		ObjectList<Tuple> enums = LookAndSort(mIndex, searchRequest);
		if (enums.size() == 0) {
			return new QueryResultSet(0);
		}

		TopQueue heap = new TopQueue(TOP_K, 0.0f);
		
		Set<Tuple> toRemove = new HashSet<>();
		int currentDocid = InitializeAndSelectMinimumDocId(enums);		
        while (enums.size() > 0) {
        	toRemove.clear();
        	//System.err.println(currentDocid);
        	Result result = new Result(currentDocid);
        	int nextDocid = Integer.MAX_VALUE;
        	for (int i = 0; i < enums.size(); i++) {
        		IterablePosting p = enums.get(i).posting;
        		if (p.getId() == currentDocid) {
        			mWeightingModel.setEntryStatistics(enums.get(i));
        			mWeightingModel.setKeyFrequency(1); //TODO: is 1 ok?
        			mWeightingModel.prepare();
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
        	heap.insert(result);
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
